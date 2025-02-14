import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class WeatherAPIData {
    public static void main(String[] args) {
        try{
            Scanner scanner = new Scanner(System.in);
            String city;
            do{
                // Четко отделяем новый ввод
                System.out.println("===================================================");
                System.out.print("Enter city (or type 'No' to exit): ");
                city = scanner.nextLine();

                if(city.equalsIgnoreCase("No")) break; // Если чел устал, выходим

                // Получаем координаты города через API да
                JSONObject cityLocationData = (JSONObject) getLocationData(city);
                double latitude = (double) cityLocationData.get("latitude");
                double longitude = (double) cityLocationData.get("longitude");

                // Показываем погоду в этом месте
                displayWeatherData(latitude, longitude);
            }while(!city.equalsIgnoreCase("No"));

        }catch(Exception e){
            e.printStackTrace(); // Если всё сломалось, хотя бы узнаем почему
        }
    }

    private static JSONObject getLocationData(String city){
        city = city.replaceAll(" ", "+"); // Если юзер ввел пробел, подменяем на '+', чтобы API не ругался

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                city + "&count=1&language=en&format=json";

        try{
            // 1. Делаем запрос к API
            HttpURLConnection apiConnection = fetchApiResponse(urlString);

            // Проверяем, всё ли ок (200 — значит норм)
            if(apiConnection.getResponseCode() != 200){
                System.out.println("Ошибка: API не отвечает :(");
                return null;
            }

            // 2. Читаем JSON-ответ в виде строки
            String jsonResponse = readApiResponse(apiConnection);

            // 3. Превращаем строку в JSON-объект
            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(jsonResponse);

            // 4. Забираем данные о городе
            JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
            return (JSONObject) locationData.get(0); // Берём первый результат, потому что он самый точный

        }catch(Exception e){
            e.printStackTrace(); // Если что-то пошло не так, будет понятно где
        }
        return null; // Если вообще ничего не получилось, возвращаем пустоту
    }

    private static void displayWeatherData(double latitude, double longitude){
        try{
            // 1. Делаем запрос на погоду по координатам
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                    "&longitude=" + longitude + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,is_day,precipitation,rain,showers,snowfall&hourly=temperature_2m";
            HttpURLConnection apiConnection = fetchApiResponse(url);

            // Проверяем, ответило ли API нормально
            if(apiConnection.getResponseCode() != 200){
                System.out.println("Ошибка: API не отвечает, попробуй позже.");
                return;
            }

            // 2. Читаем JSON-ответ
            String jsonResponse = readApiResponse(apiConnection);

            // 3. Парсим JSON в объект
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);
            JSONObject currentWeatherJson = (JSONObject) jsonObject.get("current");

            // 4. Достаём и выводим нужные данные
            String time = (String) currentWeatherJson.get("time");
            System.out.println("Current time: " + time);

            double temperature = (double) currentWeatherJson.get("temperature_2m");
            System.out.println("Current temperature (°C): " + temperature);

            long relativeHumidity = (long) currentWeatherJson.get("relative_humidity_2m");
            System.out.println("Humidity: " + relativeHumidity + "%");

            double windSpeed = (double) currentWeatherJson.get("wind_speed_10m");
            System.out.println("Wind speed: " + windSpeed + " m/s");
        }catch(Exception e){
            e.printStackTrace(); // Опять же, если что-то сломалось, надо это увидеть
        }
    }



    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            // Читаем ответ от сервера и собираем в строку
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(apiConnection.getInputStream());

            // Читаем каждую строку и добавляем в StringBuilder
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }

            scanner.close(); // Закрываем сканер, чтоб память не жрал

            return resultJson.toString(); // Отдаём JSON-строку

        } catch (IOException e) {
            e.printStackTrace(); // Выводим ошибку, если что-то пошло не так
        }

        return null; // Если сломалось — просто вернём null
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // Пробуем подключиться к API
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Говорим серверу, что нам нужен GET-запрос
            conn.setRequestMethod("GET");

            return conn;
        }catch(IOException e){
            e.printStackTrace(); // Если API не доступно, узнаем об этом
        }

        return null; // Если не смогли подключиться — возвращаем пустоту
    }
}
