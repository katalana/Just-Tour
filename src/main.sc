require: requirements.sc


#========================================= СТАРТ И ГЛОБАЛЬНЫЕ ИНТЕНТЫ ==================================================

theme: /  

    # Старт диалога с интентов *start и приветствия
    state: Welcome
        q!: *start                                  
#ПРОПИСАТЬ ИНТЕНТ ПРИВЕТСТВИЯ В КАЙЛЕ!!
        q!: $hi *
        script: $session = {}      //обнулили переменные сессии
        # проверяем есть ли имя клиента, представляемся соотв.образом и идем в Меню или Имя
        if: $client.name
            a: Здравствуйте, {{$client.name}}. Это Мария, бот Just Tour.
            go!: /Menu/Begin
        else:
            a: Здравствуйте, я Мария, бот туристической компании Just Tour.
            go!: /Name/AskName

    # Глобальный ответ на нераспознанные реплики, контекст не меняется
    state: CatchAll || noContext = true 
        event!: noMatch
        a: Простите, я не поняла. Попробуйте сказать по-другому.

    # Глобальный ответ на прощание
    state: Bye || noContext = true 
        event!: Прощание
        a: Всего доброго!

    # Глобальный стейт при выходе из сценария по желанию клиента
    state: Exit
        a: Буду на связи. Обращайтесь, если понадоблюсь!
        script: $session = {}      //обнулили переменные сессии

    # Глобальный стейт при завершении сценария после отправки заявки на тур
    state: End
        a: До свидания!
        script: $session = {}      //обнулили переменные сессии


#============================================ ЗАПРОС ИМЕНИ ===============================================================

theme: /Name

    # Запрашиваем имя, любые слова считаем именем, поэтому остаемся в этом стейте
    state: AskName || modal = true
        a: Как я могу к Вам обращаться?
   
        # имя совпало с переменной из списка - идем в Меню
        state: GetName
            q: * $Name *
            script: $client.name = $parseTree._Name.name;
            a: {{$client.name}}, приятно познакомиться!    
            go!: /Menu/Begin
        # не хочу знакомиться - соглашаемся и идем в меню
        state: NoName
            q: (* никак */* не надо */ * не хочу *)
            a: Как вам будет удобно. Обойдемся без знакомства.
            go!: /Menu/Begin
            
        # другое непонятное слово - уточняем имя это или нет
        state: GetStrangeName
            q: * 
            script: $session.probablyName = $request.query;
            a: {{$session.probablyName}}! Какое необычное имя. Вы не ошиблись? Я могу вас так называть?
            buttons:
                "Да"
                "Нет"
            # если имя - сохраняем его и идем в Меню
            state: Yes
                q: (да/* верно *)
                script: $client.name = $session.probablyName;
                a: {{$client.name}}, приятно познакомиться!
                go!: /Menu/Begin
            # если не имя - соглашаемся не знакомиться и идем в Меню
            state: No
                q: (нет/* не верно *)
                a: Как вам будет удобно. Обойдемся без знакомства.
                go!: /Menu/Begin
                
#====================================================== МЕНЮ ===========================================================

theme: /Menu
    state: Begin
        a: Я расказываю о погоде в разных городах мира и могу оформить заявку на подбор тура. 
        go!: /Menu/Choose

    # спрашиваем что выбрает
    state: Choose
        a: Что Вас интересует?
        # ДОБАВИТЬ ИНТЕНТЫ ПЕРЕДЕЛАТЬ КНОПКИ НА ЧЕТКИЕ ПЕРЕХОДЫ        
        # переход по кнопкам ведет в нужные стейты
        buttons:
            "Оформить заявку на тур"
            "Рассказать о погоде"
        # интент Что еще умеешь - идем в начало выбора       
        state: WhatElse
            q: * [что] еще [умеешь]*
            q: * другое *
            a: Я пока больше ничего не умею. Только рассказывать о погоде и оформлять заявку на тур.
            go!: /Menu/Choose
        # интент Отказ - идем на выход
        state: Deny
            q: (* ничего/отказ *)
            q: (* (не хочу)/(не надо) *)
            q: * [это] не то *
            a: Как скажете.
            go!: /Exit
        # интент город/страна 
        state: Location
            q: * $City *
            q: * $Country *
            #если назван город - запоминаем город и его координаты
            if: $parseTree.City
                script:
                    $session.place = {name: $parseTree._City.name, namesc: "", type: "городе"}
                    $session.coordinates = {lat: $parseTree._City.lat, lon: $parseTree._City.lon};
            #иначе - запоминаем страну и её координаты
            else: 
                script:
                    $session.place = {name: $parseTree._Country.name, namesc: $parseTree._Country.namesc, type: "стране"}
                    $session.coordinates = {lat: $parseTree._Country.lat, lon: $parseTree._Country.lon}
            #запрашиваем дату
            a: {{$session.place.name}}? Интересная идея! Сейчас узнаю какая там погода. Какую дату посмотреть?
            # дата названа - записываем дату и идем в прогноз
            state: Date
                q: @duckling.date
                script: $session.date = $parseTree.value;
                go!: /Weather/Begin
            # интент отказ - предлаагаем варианты и идем в меню
            state: Deny
                q: * (ничего/никакую) *
                q: (*  (не хочу)/(не надо) *)
                q: (* нет/не надо *)
                a: Хотите, оформим заявку на подбор тура? Или посмотрим погоду в другом месте.
                go!: /Menu/Choose
            # ответ непонятен - ругаемся и идем в меню
            state: NoMatch
                event: noMatch
                a: Я вас не поняла.
                go!: /Menu/Choose
                
#============================================= ПОГОДА =============================================#

theme: /Weather 
    
    state: Begin
    #вопрос из любого места о погоде
        q!: * (~погода) *
        q!: * [хочу] узнать погоду *
        q!: * (погодочку/погодку/погоду) *
        # если уже есть город/страна
        if: $session.place
            # если это город - идем на запрос даты
            if: $session.place.type == "городе"
                go!: /Weather/Step2
            # иначе спрашиваем, будет ли город, идем на обработку этого вопроса
            else:
                a: Смотрю погоду в стране {{$session.place.name}}. Можете назвать город?
                go!: /Weather/AskCity
        #если нет ни города, ни страны - запрашиваем
        else:    
            a: Назовите город или страну
        #если назвали город - записали город и идем на запрос даты    
        state: City    
            q: * $City *
            script:
                $session.place = {name: $parseTree._City.name, namesc: "", type: "городе"}
                $session.coordinates = {lat: $parseTree._City.lat, lon: $parseTree._City.lon};
            go!: /Weather/Step2
        #если назвали страну - записали страну и идем на начало чтоб спросить про город
        state: Contry
            q: * $Country *
            script:
                $session.place = {name: $parseTree._Country.name, namesc: $parseTree._Country.namesc, type: "стране"}
                $session.coordinates = {lat: $parseTree._Country.lat, lon: $parseTree._Country.lon}
            go!: /Weather/Begin
        #если название непонятное - переспрашиваем и идем на начало чтоб спросить страну или город
        state: WeatherNoMatch
            event: noMatch
            a: Простите, я не знаю такого названия
            go!: /Weather/Begin

    #обработка запроса города если есть только страна
    state: AskCity
        # назван город - сохранили его и идем смотреть прогноз
        state: City
            q: * $City *
            script:
                $session.place = {name: $parseTree._City.name, namesc: "", type: "городе"}
                $session.coordinates = {lat: $parseTree._City.lat, lon: $parseTree._City.lon};
            go!: /Weather/Step2        
        #ответ тупо "да" - просим назвать город, идем на начало обработки запроса
        state: Yes
            q: да *
            q: * [да] (могу/конечно) *
            a: Назовите же его скорее!
            go!: /Weather/AskCity
        #ответ нет - идем смотреть прогноз по стране
        state: No
            q: (* нет/не знаю/не помню *)
            q: * [нет] (не могу) *
            a: Окей, смотрим прогноз в среднем по стране
            go!: /Weather/Step2
        #любой другой ответ - тоже идем смотреть прогноз по стране    
        state: NoMatch
            event: noMatch
            a: Простите, я не знаю такого города. Посмотрю прогноз по стране
            go!: /Weather/Step2
            
    #запрос даты
    state: Step2
        #если дата уже была раньше сохранена - идем на Шаг3
        if: $session.date
            go!: /Weather/Step3
        #иначе - спрашиваем дату
        else: 
            a: На какую дату смотрим прогноз погоды?
        #названа дата - сохраняем ее и идем на Шаг3
        state: Date
            q: @duckling.date            
            script: $session.date = $parseTree.value
            go!: /Weather/Step3
        #отказ - идем на выход
        state: Deny
            q: * (ничего/никакую) *
            q: (* (не хочу)/(не надо) *)
            q: (* нет/не надо *)
            q: (* нет/отказ* *)
            a: Как скажете.
            go!: /Exit
        #любой другой ответ - подсказываем что надо ответить и идем на начало шага
        state: NoMatch
            event: noMatch
            a: Назовите число и месяц.
            go!: /Weather/Step2    
    
    #проверка даты
    state: Step3
        #вызвали текущую дату, сравнили ее с сохраненной, определили интервал прогноза
        script:
            $temp.nowDate = Date.parse ($jsapi.dateForZone ("Europe/Moscow","yyyy-MM-dd"));
            $session.interval = ($session.date.timestamp - $temp.nowDate)/60000/60/24;
            #если интервал в рамках границ прогноза - идем его запрашивать
        if: ( $session.interval == 0 || ($session.interval > 0 && $session.interval < 17) )
            go!: /Weather/ForecastStep4
        # если нет - уточняем у пользователя что он хочет
        else: 
            if: ( $session.interval == 17 ||  $session.interval > 17 )
                a: Не могу посмотреть прогноз больше, чем на 16 дней. Узнать погоду год назад в эту дату?
            else:    
                a: Эта дата в прошлом. Узнать какая погода была в эту дату в прошлом году?
        #если да - идем запрашивать исторические данные
        state: Yes
            q: Да
            go!: /Weather/HistoryStep4
        #если нет - очистили дату и идем снова спрашивать дату
        state: No
            q: * нет *
            q: (* другая/другую [~дата] *)
            q: (* (не хочу)/(не надо) *)
            script: delete $session.date
            a: Хорошо.
            go!: /Weather/Step2
        
    # функция запроса погоды
    state: ForecastStep4
        script:
            # запрашиваем погоду по API и сохраняем температуру
            $temp.weather = getWeather($session.coordinates.lat, $session.coordinates.lon, $session.interval);
            $session.TempForQuest = $temp.weather.temp;
        #если ответ пришел - выдаем его
        if: $temp.weather
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
                    $session.answerDate += $session.date.day + "." + $session.date.month + "." + $session.date.year;
                }
            # формируем ответ про город со словом город или про страну в дательном падеже
            if: $session.place.type == "городе"        
                a: Погода в {{$session.place.type}} {{$session.place.name}} на {{$session.answerDate}}: {{($temp.weather.descript).toLowerCase()}}, {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
            else:
                a: Погода в {{$session.place.namesc}} на {{$session.answerDate}}: {{$temp.weather.descript}}, {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
        # если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!: /Menu/Begin
        #идем спрашивать клиента про климат
        go!: /Weather/AreYouSure
    
    # функция запроса исторических данных о погоде
    state: HistoryStep4
        script:
            # формируем для запроса входную и выходную дату в прошлом году
            $session.historyDay1 = "";
            $session.historyDay2 = "";
            $session.historyDay1 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + $session.date.day;
            $session.historyDay2 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + plus($session.date.day);
            # запрашиваем погоду  по API и сохраняем температуру
            $temp.weather = getHistoricalWeather($session.coordinates.lat, $session.coordinates.lon, $session.historyDay1, $session.historyDay2);
            $session.TempForQuest = $temp.weather.temp;
        # если ответ пришел - выдаем его
        if: $temp.weather
            # формируем ответ про день/дату, на который получен прогноз            
            script: 
                $session.answerDate = "";
                $session.answerDate += $session.date.day + "." + $session.date.month; 
            # формируем ответ про город со словом город или про страну в дательном падеже
            if: $session.place.type == "городе"        
                a: Погода в {{$session.place.type}} {{$session.place.name}} в прошлом году на {{$session.answerDate}} была: {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
            else:
                a: Погода в {{$session.place.namesc}} в прошлом году на {{$session.answerDate}} была: {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
        # если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!: /Menu/Begin
        #идем спрашивать клиента про климат
        go!: /Weather/AreYouSure    

    state: AreYouSure
        #уточняем: точно ли клиент хочет поехать в умеренный/холодный/теплый климат
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

            # state: New
            #     q: * $comYes *
            #     q: верно
            #     q: * [это] (он/оно/то что нужно) *
            #     q: * (новый/новую/нового/сначала) *
            #     q: * (заного/заново) *
            #     script:
            #         #Начальная точка оформления заявки:
            #         delete $session.StartPoint;
            #         #Город отправления:
            #         delete $session.departureCity;
            #         #Дата отправления:
            #         delete $session.departureDate;
            #         #Дата возвращения:
            #         delete $session.returnDate;
            #         #Количество людей:
            #         delete $session.people;
            #         #Количество детей:
            #         delete $session.children;
            #         #Бюджет:
            #         delete $session.bablo;
            #         #Звезд у отеля:
            #         delete $session.stars;
            #         #Комментарий для менеджера:
            #         delete $session.comments;
            #     go!: /Trip/TripStartPoint
        
            # state: Old
            #     q: * (~продолжить) *
            #     q: * (~старая/старую/~прошлая/прошлую/прошлый/старой/предыдущей/предыдушую) *
            #     go!: /Trip/TripStartPoint
        
        state: NoSure
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * [это] не то [что я хотел] *
            a: Хотите узнать о погоде в другом Городе/Стране?

            # state: YesNoSure
            #     q: * $comYes *
            #     q: верно
            #     q: * [это] (он/оно/то что нужно) *
            #     script:
            #         delete $session.arrivalPointForCity;
            #         delete $session.arrivalPointForCountry;
            #         delete $session.arrivalCoordinates; 
            #     go!: /Weather/WeatherQust  
        
            # state: NoNoSure
            #     q: * $comNo *
            #     q: * (не верно/неверно) *
            #     q: * [это] не то [что я хотел] *
            #     a: К сожалению я больше ничем не могу Вам помочь.


#============================================= ОФОРМЛЕНИЕ ПУТЕВКИ =============================================#

# theme:/Trip
    
    
#     # Если уже есть имя - проверяем актуально ли оно
#     state: ConfirmName
#         #|| modal = true
#         a: Ваше имя {{$client.name}}, верно?
#         script: $session.probablyName = $client.name;
#         buttons:
#             "Да"
#             "Нет"
#         # если да - сохраняем имя и очищаем переменную варианта имени     
#         state: Yes
#             q: (да/* верно *)
#             script: $client.name = $session.probablyName;
#             a: {{$client.name}}, приятно познакомиться!
# #ИЗМЕНИТЬ ПЕРЕХОД
#             # go!: /Menu/Begin
#         # если нет - снова идем снова спрашивать имя
#         state: No
#             q: (нет/не [верно])
# #ИЗМЕНИТЬ ПЕРЕХОД
#             # go!: /Name/AskName
#         # Если нет внятного ответа - возвращаемся к вопросу    
#         state: CatchAll || noContext = true 
#             event: noMatch
#             a: Пожалуйста, ответьте да или нет:
#             go!: /Name/ConfirmName        
# #ДОБАВИТЬ ИНТЕНТ ОТКАЗА, желательно зацепить его на вторую ситуацию отказа из схемы
#         state: DenyName
    
    
    
    
    
    
    
#     state: TripStartPoint
#         q!: * (~Путевка/тур*/туристичес*/подбери (тур/путевку)) * 
#         if: $session.StartPoint
#             if: $session.departureCity
#                 go!: /Trip/TripStartPoint/DepartureCity 
#             else:
#                 a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.   
#         else:
#             a: {{ $client.name }}, назовите, пожалуйста, город отправления. Если Вы ошибетесь в данных - не волнуйтесь, наш менеджер свяжется с Вами для уточнения данных.
#             script:
#                 $session.StartPoint = 1;
    
#         state: DepartureCity
#             q: * $City *
#             if: $session.departureCity 
#                 if: $session.departureDate
#                     go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
#                 else:
#                     a: Желаемая дата отправления?
#             else:
#                 script:
#                     $session.departureCity = $parseTree._City.name;
#                 if: $session.departureDate
#                     go!: /Trip/TripStartPoint/DepartureCity/DepartureDate
#                 else:
#                     a: Желаемая дата отправления?
            
            
#             state: DepartureDate
#                 q: * (@duckling.date/@duckling.time) *
#                 if: $session.departureDate
#                     if: $session.arrivalPointForCity  
#                         go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
#                     elseif: $session.arrivalPointForCountry 
#                         go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
#                     else:
#                         a: Куда бы вы хотели отправиться?
#                 else:
#                     script:
#                         $session.departureDate = $parseTree.value;
#                     if: $session.arrivalPointForCity  
#                         go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCity
#                     elseif: $session.arrivalPointForCountry 
#                         go!: /Trip/TripStartPoint/DepartureCity/DepartureDate/ArrivalCountry
#                     else:
#                         a: Куда бы вы хотели отправиться?
                
                
#                 state: ArrivalCity
#                     q: * $City *
#                     if: $session.arrivalPointForCity  
#                         if: $session.returnDate
#                             go!: /Trip/ReturnDate
#                         else:
#                             a: Желаемая дата возвращения?
#                             go: /Trip/ReturnDate
#                     else:
#                         script:
#                             $session.arrivalPointForCity = $parseTree._City.name;
#                         if: $session.returnDate
#                             go!: /Trip/ReturnDate
#                         else:
#                             a: Желаемая дата возвращения?
#                             go: /Trip/ReturnDate
                
                
#                 state: ArrivalCountry
#                     q: * $Country *
#                     if: $session.arrivalPointForCountry  
#                         if: $session.returnDate
#                             go!: /Trip/ReturnDate
#                         else:
#                             a: Желаемая дата возвращения?
#                             go: /Trip/ReturnDate
#                     else:
#                         script:
#                             $session.arrivalPointForCountry = $parseTree._Country.name;
#                         if: $session.returnDate
#                             go!: /Trip/ReturnDate
#                         else:
#                             a: Желаемая дата возвращения?
#                             go: /Trip/ReturnDate
                
                
#                 state: CatchAll || noContext = true
#                     event: noMatch
#                     a: Простите, Но туда у нас нет туров. Выберете другую точку.

#     state: ReturnDate            
#         q: * (@duckling.date/@duckling.time) *
#         if: $session.returnDate
#             if: $session.people
#                 go!: /Trip/ReturnDate/People
#             else:
#                 a: Количество людей в поездке?
#         else:
#             script:
#                 $session.returnDate = $parseTree.value;
#             if: $session.people
#                 go!: /Trip/ReturnDate/People
#             else:
#                 a: Количество людей в поездке? 
        
#         buttons:
#             "уточню позже"    
            
#         state: People
#             q: *
#             if: $session.people
#                 if: $session.children
#                     go!: /Trip/ReturnDate/People/Children
#                 else:
#                     a: Количество детей в поездке?
#             else:
#                 script:
#                     $session.people = $request.query;
#                 if: $session.children
#                     go!: /Trip/ReturnDate/People/Children
#                 else:
#                     a: Количество детей в поездке?
            
#             buttons:
#                 "уточню позже"    
            
#             state: Children
#                 q: * 
#                 if: $session.children
#                     if: $session.bablo
#                         go!: /Trip/ReturnDate/People/Children/Bablo
#                     else:
#                         a: Какой у Вас Бюджет?
#                 else:
#                     script:
#                         $session.children = $request.query;
#                     if: $session.bablo
#                         go!: /Trip/ReturnDate/People/Children/Bablo
#                     else:
#                         a: Какой у Вас Бюджет?
                 
#                 buttons:
#                     "уточню позже"    
            
#                 state: Bablo
#                     q: *
#                     if: $session.bablo
#                         if: $session.stars
#                             go!: /Trip/ReturnDate/People/Children/Bablo/Stars
#                         else:
#                             a: Скольки звездочный отель ?
#                     else:
#                         script:
#                             $session.bablo = $request.query;
#                         if: $session.stars
#                             go!: /Trip/ReturnDate/People/Children/Bablo/Stars
#                         else:
#                             a: Скольки звездочный отель ?
            
#                     buttons:
#                         "уточню позже"    
            
#                     state: Stars
#                         q: *
#                         if: $session.stars
#                             if: $session.comments
#                                 go!: /Trip/ReturnDate/People/Children/Bablo/Stars/Comments
#                             else:
#                                 a: Комментарий для менеджера?
#                         else:
#                             script:
#                                 $session.stars = $request.query;
#                             if: $session.comments
#                                 go!: /Trip/ReturnDate/People/Children/Bablo/Stars/Comments
#                             else:
#                                 a: Комментарий для менеджера?
                 
#                         buttons:
#                             "нет комментариев"    
            
#                         state: Comments
#                             q: *
#                             if: $session.comments
#                                 go!: /FullData/Screen
#                             script:
#                                 $session.comments = $request.query;
#                             go!: /FullData/Screen

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


# theme: /Phone
#     state: Ask 
#         a: Для передачи заявки менеджеру, мне нужен Ваш номер телефона в формате 79000000000.

#         state: Get
#             q: $phone
#             go!: /Phone/Confirm

#         state: Wrong
#             q: *
#             a: О-оу. ошибка в формате набора номера. Проверьте.
#             go!: /Phone/Ask


#     state: Confirm
#         script:
#             $temp.phone = $parseTree._phone || $client.phone;

#         a: Ваш номер {{ $temp.phone }}, верно?

#         script:
#             $session.probablyPhone = $temp.phone;

#         buttons:
#             "Да"
#             "Нет"

#         state: Yes
#             q: (да/верно)
#             script:
#                 $client.phone = $session.probablyPhone;
#                 delete $session.probablyPhone;
#             go!: /SendMail/Mail
            

#         state: No
#             q: (нет/не [верно])
#             go!: /Phone/Ask            
            

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
