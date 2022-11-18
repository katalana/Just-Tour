require: requirements.sc

# init:     //путь к текущему стейту будет везде сохраняться в $session как предыдущий стейт
#     bind(
#         "postProcess", 
#         function($context) {
#         $context.session.lastState = $context.currentState;
#         }
#     );


theme: /  

# Старт диалога с глобальных тегов активации q!: слово *Start или переменная $hi
    state: Welcome
        q!: *start                                  
        q!: $hi *
        script: $session = {}      //обнулили переменные сессии
# проверяем есть ли имя клиента, представляемся соотв.образом и идем в соотв.стейт
        if: $client.name
            a: Здравствуйте, {{$client.name}}. Это Мария, бот Just Tour.
            go!: /Name/ConfirmName
            # go!: /Service/SuggestHelp
        else:
            a: Здравствуйте, я Мария, бот туристической компании Just Tour.
            go!: /Name/AskName

# Глобальный ответ на нераспознанные реплики, контекст не меняется
    state: CatchAll || noContext = true 
        event!: noMatch
        a: Простите, я не поняла. Попробуйте написать по другому.



theme: /Name

# Если нет имени - запрашиваем его, на любые ненужные реплики стоим в рамках этого стейта
    state: AskName || modal = true
        a: Как я могу к Вам обращаться?
   
# имя совпало с переменной $Name - идем на старт диалога
        state: GetName
            q: * $Name *
            script:
                $client.name = $parseTree._Name.name;
            a: {{ $client.name }}, приятно познакомиться!    
            go!: /Service/SuggestHelp
# не хочу знакомиться - соглашаемся и идем на старт диалога
        state: NoName
            q: (* никак */* не надо */ * не хочу *)
            a: Как вам будет удобно. Обойдемся без знакомства.
            go!: /Service/SuggestHelp
            
# непонятный ответ - уточняем имя это или нет
        state: GetStrangeName
            q: * 
            script:
                $session.probablyName = $request.query;
            a: {{$session.probablyName}}! Какое необычное имя. Вы не ошиблись? Я могу вас так называть?
            buttons:
                "Да"
                "Нет"
# если имя - сохраняем имя и очищаем переменную варианта имени            
            state: Yes
                q: (да/* верно *)
                script:
                    $client.name = $session.probablyName;
                    delete $session.probablyName;
                a: {{ $client.name }}, приятно познакомиться!
                go!: /Service/SuggestHelp
# если не имя - соглашаемся не знакомиться и идем на старт диалога
            state: No
                q: (нет/* не верно *)
                a: Как вам будет удобно. Обойдемся без знакомства.
                go!: /Service/SuggestHelp
                
# Если уже есть имя - проверяем актуально ли оно
    state: ConfirmName
        #|| modal = true
        a: Ваше имя {{ $client.name }}, верно?
        script:
            $session.probablyName = $client.name;
        buttons:
            "Да"
            "Нет"

# если да - сохраняем имя и очищаем переменную варианта имени     
        state: Yes
            q: (да/верно)
            script:
                $client.name = $session.probablyName;
                delete $session.probablyName;
            a: {{ $client.name }}, приятно познакомиться!
            go!: /Service/SuggestHelp
# если нет - снова идем снова спрашивать имя
        state: No
            q: (нет/не [верно])
            go!: /Name/AskName
# Если нет внятного ответа - возвращаемся к вопросу    
        state: CatchAll || noContext = true 
            event: noMatch
            a: Пожалуйста, ответьте да или нет:
            go!: /Name/ConfirmName            
            
            
#============================================= Базовый запрос на погоду и путевку =============================================#

theme: /Service
    state: SuggestHelp
        a: Я расказываю о погоде в разных городах мира и могу оформить заявку на подбор тура. 
        a: Что Вас интересует?
# ДОБАВИТЬ ИНТЕНТЫ ПЕРЕДЕЛАТЬ КНОПКИ НА ЧЕТКИЕ ПЕРЕХОДЫ        
        buttons:
            "Оформить заявку на тур"
            "Рассказать о погоде"
            
#============================================= ПОГОДА =============================================#

theme: /Weather 
    
    state: WeatherQust 
        #вопрос из любого места о погоде без конкретики. Если до этого погоду уже спрашивали, то уточнит по месту. 
        q!: * (~погода) *
        q!: * [хочу] узнать погоду *
        q!: * {погоду подскажи} *
        q!: * {(погодочку/погодку/погоду) [бы]}
# проверяем есть ли город/страна, уточняем, там ли нужна погода, если нет - спрашиваем где нужна погода
        if: $session.arrivalPointForCity
            a: Вы хотели бы узнать погоду в {{ $session.arrivalPointForCity }}?
        elseif: $session.arrivalPointForCountry
            a: Вы хотели бы узнать погоду в {{ $session.arrivalPointForCountry }}?
        else:
            a: В Каком Городе/Стране Вас интересует погода?
# если да, идем запрашивать погоду
        state: WeatherYes
            q: * $comYes *
            q: верно
            q: * [это] (он/оно/то что нужно) *
            if: $session.arrivalPointForCity
                go!: /Weather/WeatherOfCity
            elseif: $session.arrivalPointForCountry
                go!: /Weather/WeatherOfCountry
# если нет, очищаем переменную, возвращаемся к запросу
        state: WeatherNO
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * [это] не то [что я хотел] *
            script:
                delete $session.arrivalPointForCity;
                delete $session.arrivalPointForCountry;
                delete $session.arrivalCoordinates;
            go!: /Weather/WeatherQust
#если введен город - идем запрашивать погоду по городу
# ГДЕ СОХРАНЕНИЕ ПЕРЕМЕННОЙ?!
        state: WeatherCity
            q: * $City *
            script: $session.typeOfPlace = "городе"
            go!: /Weather/WeatherInTheCity
#если введена страна - идем запрашивать погоду по стране
# ГДЕ СОХРАНЕНИЕ ПЕРЕМЕННОЙ?!
        state: WeatherCountry
            q: * $Country *
            script: $session.typeOfPlace = "стране"
            go!: /Weather/WeatherInTheCountry
    
    state: WeatherInTheCity
        #вопрос из любого места о погоде в конкретном городе
        q!: * [$Question] * $Weather * $City *
        q!: * [$Question] * $City * $Weather *
        q!: * а в $City *
#распарсили ответ на имя города и координаты
        script:
            $session.arrivalPointForCity = $parseTree._City.name;
            $session.arrivalCoordinates = {
                lat: $parseTree._City.lat,
                lon: $parseTree._City.lon
            };
#идем спрашивать дату
        go!: /Weather/Date

        
#запрос даты
    state: Date
        a: На какую дату смотрим прогноз?

        state: InputDate
#если введена дата
            q: @duckling.date
#определяем горизонт прогноза
            script:
                $session.Date = $parseTree.value;
#ДОБАВИТЬ ОПРЕДЕЛЕНИЕ ЧАСОВОГО ПОЯСА!
                var current = $jsapi.dateForZone("Europe/Moscow","yyyy-MM-dd");
                current = Date.parse (current);
                $session.interval = ($session.Date.timestamp-current)/60000/60/24;
#проверям, попадает ли горизонт в рамки прогноза и выбираем что показать - прогноз или историю
            if: ($session.interval == 0 || ($session.interval > 0 && $session.interval < 17))
                go!: /Weather/WeatherRequest
            else: 
                if: ( $session.interval == 17 ||  $session.interval > 17 )
                    a: Не могу посмотреть прогноз больше, чем на 16 дней. Узнать погоду год назад в эту дату?
                else:    
                    a: Эта дата в прошлом. Узнать какая погода была в эту дату в прошлом году?
#ловим ответ да/нет/noMatch
            state: Yes
                q: Да
                a: была введена дата {{toPrettyString($session.Date)}}
# идем запрашивать исторические данные
                go!: /Weather/WeatherHistoryRequest
            state: No
                q: Нет
                a: Хорошо
                go!: /Weather/Date
            state: NoMatch
                q: *
                a: Извините, я вас не поняла
                go!: /Weather/Date
        
        state: InputNoData
#если дата не введена        
            q: *
            a: Я не могу посмотреть прогноз без даты
            go!: /Weather/Date
        
# функция запроса погоды
    state: WeatherRequest
        script:
# запрашиваем погоду по API и сохраняем температуру в сессионную переменную
            $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon, $session.interval);
            $session.TempForQuest = $temp.Weather.temp;
# если ответ пришел - выдаем его
        if: $temp.Weather
# формируем ответ про день/дату, на который получен прогноз
            script: 
                if ($session.interval == 0) {
                    $session.answerDate = "сегодня";
                }
                else if ($session.interval == 1) {
                    $session.answerDate = "завтра";
                }
                else {
                    $session.answerDate = "";
                    $session.answerDate += $session.Date.day + "." + $session.Date.month + "." + $session.Date.year;
                }
            a: Погода в {{ $session.typeOfPlace }} {{ $session.arrivalPointForCity }} на {{$session.answerDate}}: {{ $temp.Weather.description }}, {{ $temp.Weather.temp }}°C. Ветер {{ $temp.Weather.wind }} м/с, порывы до {{ $temp.Weather.gust }} м/с.
# если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!:  /Service/SuggestHelp
#идем спрашивать клиента про климат
        go!: /Weather/AreYouSure
    
# функция запроса погоды в прошлом    
    state: WeatherHistoryRequest
        script:
# формируем для запроса входную и выходную дату в прошлом году
            $session.historyDay1 = "";
            $session.historyDay2 = "";
            $session.historyDay1 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.Date.month + "-" + $session.Date.day;
            $session.historyDay2 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.Date.month + "-" + plus($session.Date.day);
# запрашиваем погоду  по API и сохраняем температуру в сессионную переменную
            $temp.Weather = getHistoricalWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon, $session.historyDay1, $session.historyDay2);
            $session.TempForQuest = $temp.Weather.temp;
# если ответ пришел - выдаем его
        if: $temp.Weather
# формируем ответ про день/дату, на который получен прогноз            
            script: 
                $session.answerDate = "";
                $session.answerDate += $session.Date.day + "." + $session.Date.month; 
            a: Погода в {{ $session.typeOfPlace }} {{ $session.arrivalPointForCity }} в прошлом году на {{$session.answerDate}} была: {{ $temp.Weather.temp }}°C. Ветер {{ $temp.Weather.wind }} м/с, порывы до {{ $temp.Weather.gust }} м/с.
# если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!:  /Service/SuggestHelp
#идем спрашивать клиента про климат
        go!: /Weather/AreYouSure    

    
    # state: WeatherInTheCountry 
    #     #вопрос из любого места о погоде в конкретной стране
    #     q!: * [$Question] * $Weather * $Country *
    #     q!: * [$Question] * $Country * $Weather *
    #     q!: * а в $Country *
    #     script:
    #         $session.arrivalPointForCountry = $parseTree._Country.namesc;
    #         $session.arrivalCoordinates = {
    #             lat: $parseTree._Country.lat,
    #             lon: $parseTree._Country.lon
    #         };
    #         $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
    #         $session.TempForQuest = $temp.Weather.temp;
    #     if: $temp.Weather
    #         a: В {{ $session.arrivalPointForCountry }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
    #         go!: /Weather/AreYouSure
    

    # state: WeatherOfCity 
    #     #для варианта когда уже был известен город и сессионая переменная уже обозначена
    #     script:
    #         $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
    #         $session.TempForQuest = $temp.Weather.temp;
    #     if: $temp.Weather
    #         a: В городе {{ $session.arrivalPointForCity }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
    #         go!: /Weather/AreYouSure
        
            
    # state: WeatherOfCountry 
    #     #для варианта когда уже была известна страна и сессионая переменная уже обозначена
    #     script:
    #         $temp.Weather = getWeather($session.arrivalCoordinates.lat, $session.arrivalCoordinates.lon);
    #         $session.TempForQuest = $temp.Weather.temp;
    #     if: $temp.Weather
    #         a: В {{ $session.arrivalPointForCountry }} сейчас {{ $temp.Weather.description }} {{ $temp.Weather.temp }}°C. Ощущается как {{ $temp.Weather.feelslike }}°C.
    #         go!: /Weather/AreYouSure
        
        
    state: AreYouSure
        #уточнение точно клиент хочет туда поехать?
        script:
            if ($session.TempForQuest < 25 && $session.TempForQuest > 0) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с умеренным климатом?");
            }
             else if ($session.TempForQuest < 0 || $session.TempForQuest == 0) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с холодным климатом?");
            }
            else if ($session.TempForQuest > 25 || $session.TempForQuest == 25) {
                $reactions.answer("Вы хотели бы запланировать поездку в страну с жарким климатом?");
            }

        state: YesSure
            q: * $comYes *
            q: верно
            q: * [это] (он/оно/то что нужно) *
            if: $session.StartPoint
                a: Вы хотели бы начать оформление нового тура в данную страну или хотели бы продолжить оформление старой заявки? 
            else: 
                a: Вы хотели бы начать оформление нового тура в данную страну? 

            state: New
                q: * $comYes *
                q: верно
                q: * [это] (он/оно/то что нужно) *
                q: * (новый/новую/нового/сначала) *
                q: * (заного/заново) *
                script:
                    #Начальная точка оформления заявки:
                    delete $session.StartPoint;
                    #Город отправления:
                    delete $session.departureCity;
                    #Дата отправления:
                    delete $session.departureDate;
                    #Дата возвращения:
                    delete $session.returnDate;
                    #Количество людей:
                    delete $session.people;
                    #Количество детей:
                    delete $session.children;
                    #Бюджет:
                    delete $session.bablo;
                    #Звезд у отеля:
                    delete $session.stars;
                    #Комментарий для менеджера:
                    delete $session.comments;
                go!: /Trip/TripStartPoint
        
            state: Old
                q: * (~продолжить) *
                q: * (~старая/старую/~прошлая/прошлую/прошлый/старой/предыдущей/предыдушую) *
                go!: /Trip/TripStartPoint
        
        state: NoSure
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * [это] не то [что я хотел] *
            a: Хотите узнать о погоде в другом Городе/Стране?

            state: YesNoSure
                q: * $comYes *
                q: верно
                q: * [это] (он/оно/то что нужно) *
                script:
                    delete $session.arrivalPointForCity;
                    delete $session.arrivalPointForCountry;
                    delete $session.arrivalCoordinates; 
                go!: /Weather/WeatherQust  
        
            state: NoNoSure
                q: * $comNo *
                q: * (не верно/неверно) *
                q: * [это] не то [что я хотел] *
                a: К сожалению я больше ничем не могу Вам помочь.


#============================================= ОФОРМЛЕНИЕ ПУТЕВКИ =============================================#

theme:/Trip
    
    state: TripStartPoint
        q!: * (~Путевка/тур*/туристичес*/подбери (тур/путевку)) * 
        if: $session.StartPoint
            if: $session.departureCity
                go!: /Trip/TripStartPoint/DepartureCity 
            else:
                a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.   
        else:
            a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.
            script:
                $session.StartPoint = 1;
    
        state: DepartureCity
            q: * $City *
            if: $session.departureCity 
                if: $session.departureDate
                    go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
                else:
                    a: Желаемая дата отправления?
            else:
                script:
                    $session.departureCity = $parseTree._City.name;
                if: $session.departureDate
                    go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
                else:
                    a: Желаемая дата отправления?
            
            
            state: DepartureDate
                q: * (@duckling.date/@duckling.time) *
                if: $session.departureDate
                    if: $session.arrivalPointForCity  
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
                    elseif: $session.arrivalPointForCountry 
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
                    else:
                        a: Куда бы вы хотели отправиться?
                else:
                    script:
                        $session.departureDate = $parseTree.value;
                    if: $session.arrivalPointForCity  
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
                    elseif: $session.arrivalPointForCountry 
                        go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
                    else:
                        a: Куда бы вы хотели отправиться?
                
                
                state: ArrivalCity
                    q: * $City *
                    if: $session.arrivalPointForCity  
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                    else:
                        script:
                            $session.arrivalPointForCity = $parseTree._City.name;
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                
                
                state: ArrivalCountry
                    q: * $Country *
                    if: $session.arrivalPointForCountry  
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                    else:
                        script:
                            $session.arrivalPointForCountry = $parseTree._Country.name;
                        if: $session.returnDate
                            go!: /Trip/ReturnDate
                        else:
                            a: Желаемая дата возвращения?
                            go: /Trip/ReturnDate
                
                
                state: CatchAll || noContext = true
                    event: noMatch
                    a: Простите, Но туда у нас нет туров. Выберете другую точку.

    state: ReturnDate            
        q: * (@duckling.date/@duckling.time) *
        if: $session.returnDate
            if: $session.people
                go!: /Trip/ReturnDate/People
            else:
                a: Количество людей в поездке?
        else:
            script:
                $session.returnDate = $parseTree.value;
            if: $session.people
                go!: /Trip/ReturnDate/People
            else:
                a: Количество людей в поездке? 
        
        buttons:
            "уточню позже"    
            
        state: People
            q: *
            if: $session.people
                if: $session.children
                    go!: /Trip/ReturnDate/People/Children
                else:
                    a: Количество детей в поездке?
            else:
                script:
                    $session.people = $request.query;
                if: $session.children
                    go!: /Trip/ReturnDate/People/Children
                else:
                    a: Количество детей в поездке?
            
            buttons:
                "уточню позже"    
            
            state: Children
                q: * 
                if: $session.children
                    if: $session.bablo
                        go!: /Trip/ReturnDate/People/Children/Bablo
                    else:
                        a: Какой у Вас Бюджет?
                else:
                    script:
                        $session.children = $request.query;
                    if: $session.bablo
                        go!: /Trip/ReturnDate/People/Children/Bablo
                    else:
                        a: Какой у Вас Бюджет?
                 
                buttons:
                    "уточню позже"    
            
                state: Bablo
                    q: *
                    if: $session.bablo
                        if: $session.stars
                            go!: /Trip/ReturnDate/People/Children/Bablo/Stars
                        else:
                            a: Скольки звездочный отель ?
                    else:
                        script:
                            $session.bablo = $request.query;
                        if: $session.stars
                            go!: /Trip/ReturnDate/People/Children/Bablo/Stars
                        else:
                            a: Скольки звездочный отель ?
            
                    buttons:
                        "уточню позже"    
            
                    state: Stars
                        q: *
                        if: $session.stars
                            if: $session.comments
                                go!: /Trip/ReturnDate/People/Children/Bablo/Stars/Comments
                            else:
                                a: Комментарий для менеджера?
                        else:
                            script:
                                $session.stars = $request.query;
                            if: $session.comments
                                go!: /Trip/ReturnDate/People/Children/Bablo/Stars/Comments
                            else:
                                a: Комментарий для менеджера?
                 
                        buttons:
                            "нет комментариев"    
            
                        state: Comments
                            q: *
                            if: $session.comments
                                go!: /FullData/Screen
                            script:
                                $session.comments = $request.query;
                            go!: /FullData/Screen

#============================================= Вывод данных для пользователя и возможность изменить их =============================================#

            
theme: /FullData               
    
    state: Screen
        
        script:
            var answer = "";
            answer += $client.name + ", подскажите, все ли верно:" + "\n";
            answer += "Пункт отправления: " + $session.departureCity + "\n";
            answer += "Дата отправления: " + $session.departureDate.year + "." + $session.departureDate.month + "." + $session.departureDate.day + "\n";
            if ($session.arrivalPointForCity) {
                answer += "Пункт назначения: " + $session.arrivalPointForCity + "\n";
            }
            if ($session.arrivalPointForCountry) {
                answer += "Пункт назначения: " + $session.arrivalPointForCountry + "\n";
            }
            answer += "Дата возвращения: " + $session.returnDate.year + "." + $session.returnDate.month + "." + $session.returnDate.day + "\n";
            answer += "Количество людей: " + $session.people + "\n";
            answer += "Количество детей: " + $session.children + "\n";
            answer += "Бюджет: " + $session.bablo + "\n";
            answer += "Звезд у отеля: " + $session.stars + "\n";
            answer += "Комментарии для Менеджера: " + $session.comments + "\n";
            $reactions.answer(answer);

        buttons:
            "верно"  
            "заполнить заново"
            "поменять данные"
        
        state: Yes
            q: * (да/верно) *
            go!: /SendMail/Mail

            # a: Осталось самую малость.  
            # if: $client.phone
            #   go!: /Phone/Confirm
            # else:
            #     go!: /Phone/Ask

        # state: No
        #     q: * заполнить заново *
        #     script:
        #         delete $session.StartPoint;
        #         delete $session.departureCity;
        #         delete $session.departureDate;
        #         delete $session.arrivalPointForCity;
        #         delete $session.arrivalPointForCountry;
        #         delete $session.returnDate;
        #         delete $session.people;
        #         delete $session.children;
        #         delete $session.bablo;
        #         delete $session.stars;
        #         delete $session.comments;
        #     go!: /Trip/TripStartPoint

#============================================= Запрос телефона и окончание формирования путевки =============================================#


theme: /Phone
    state: Ask 
        a: Для передачи заявки менеджеру, мне нужен Ваш номер телефона в формате 79000000000.

        state: Get
            q: $phone
            go!: /Phone/Confirm

        state: Wrong
            q: *
            a: О-оу. ошибка в формате набора номера. Проверьте.
            go!: /Phone/Ask


    state: Confirm
        script:
            $temp.phone = $parseTree._phone || $client.phone;

        a: Ваш номер {{ $temp.phone }}, верно?

        script:
            $session.probablyPhone = $temp.phone;

        buttons:
            "Да"
            "Нет"

        state: Yes
            q: (да/верно)
            script:
                $client.phone = $session.probablyPhone;
                delete $session.probablyPhone;
            go!: /SendMail/Mail
            

        state: No
            q: (нет/не [верно])
            go!: /Phone/Ask            
            

#============================================= Отправка формы на почту менеджеру =============================================#            
        
theme: /SendMail
    state: Mail
        script:
            var ClientData = "";
            var Subject = "Заявка от клиента " + $client.name;
            ClientData += " Имя: " + $client.name + "<br>";
            ClientData += " Пункт отправления: " + $session.departureCity + "<br>";
            ClientData += " Дата отправления: " + $session.departureDate.year + "." + $session.departureDate.month + "." + $session.departureDate.day + "<br>";
            if ($session.arrivalPointForCity) {
                ClientData += " Пункт назначения: " + $session.arrivalPointForCity + "<br>";
            }
            if ($session.arrivalPointForCountry) {
                ClientData += " Пункт назначения: " + $session.arrivalPointForCountry + "<br>";
            }
            ClientData += " Дата возвращения: " + $session.returnDate.year + "." + $session.returnDate.month + "." + $session.returnDate.day + "<br>";
            ClientData += " Количество людей: " + $session.people + "<br>";
            ClientData += " Количество детей: " + $session.children + "<br>";
            ClientData += " Бюджет: " + $session.bablo + "<br>";
            ClientData += " Звезд у отеля: " + $session.stars + "<br>";
            ClientData += " Комментарии для Менеджера: " + $session.comments + "<br>";
            ClientData += " Телефон: " + $client.phone + "<br>";
            $session.result = $mail.send({
                from: "katalana@mail.ru",
                to: ["katalana@mail.ru"],
                subject: Subject,
                content: ClientData,
                smtpHost: "smtp.mail.ru",
                smtpPort: "465",
                user: "katalana@mail.ru",
                password: "rRk8AEUQ6ZMAJaZ8BGEu"
            });
        if: ($session.result.status == "OK")
            a: Фсе получилось
        else:
            a: Письмо не ушло :((
