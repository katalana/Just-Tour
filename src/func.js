function getWeather(lat, lon, day) {
	var response = $http.get("https://api.weatherbit.io/v2.0/forecast/daily?lat=${lat}&lon=${lon}&lang=${lang}&key=${key}&days=16", {
            timeout: 10000,
            query:{
                key: "0ee3493daca046aea3dbe81b076f4083",
                lang: "ru",
                lat: lat,
                lon: lon,
                days: toPrettyString(day)
            }
        });

	if (!response.isOk || !response.data) {
		return false;
	}

	var weather = {};

	weather.temp = response.data.data[day].temp;
	weather.wind = response.data.data[day].wind_spd;
	weather.gust = response.data.data[day].wind_gust_spd;
	weather.description = response.data.data[day].weather.description;

	return weather;
	
}

function getHistoricalWeather(lat, lon, start_date, end_date) {
	var response = $http.get("https://api.weatherbit.io/v2.0/history/daily?lat=${lat}&lon=${lon}&start_date=${start_date}&end_date=${end_date}&key=${key}", {
            timeout: 10000,
            query:{
                key: "0ee3493daca046aea3dbe81b076f4083",
                lat: lat,
                lon: lon,
                start_date: toPrettyString(start_date),
                end_date:  toPrettyString(end_date)
            }
        });

	if (!response.isOk || !response.data) {
//		return false;
        return response;		
	}

	var weather = {};

	weather.temp = response.data.data[0].temp;
	weather.wind = response.data.data[0].wind_spd;
	weather.gust = response.data.data[0].wind_gust_spd;

	return weather;
	
}


function minus(num) {
	var number = parseInt(num);
	return (number - 1);
}	

function plus(num) {
	var number = parseInt(num);
	return (number + 1);
}	