require: requirements.sc

#========================================= СТАРТ И ГЛОБАЛЬНЫЕ ИНТЕНТЫ ==================================================
theme: /  

    #старт диалога с интентов *start и приветствия
    state: Welcome
        q!: *start                                  
#ПРОПИСАТЬ ИНТЕНТ ПРИВЕТСТВИЯ В КАЙЛЕ!!
        q!: $hi *
        script: $session = {}      //обнулили переменные сессии
        #проверяем есть ли имя клиента, представляемся соотв.образом и идем в Меню или Имя
        if: $client.name
            a: Здравствуйте, {{$client.name}}. Это Мария, бот Just Tour.
            go!: /Menu/Begin
        else:
            a: Здравствуйте, я Мария, бот туристической компании Just Tour.
            go!: /Name/AskName

    #глобальный ответ на нераспознанные реплики, контекст не меняется
    state: CatchAll || noContext = true 
        event!: noMatch
        a: Простите, я не поняла. Попробуйте сказать по-другому.

    #глобальный ответ на прощание
    state: Bye || noContext = true 
        event!: Прощание
        a: Всего доброго!

    #глобальный стейт при выходе из сценария по желанию клиента
    state: Exit
        a: Буду на связи. Обращайтесь, если понадоблюсь!
        script: $session = {}      //обнулили переменные сессии

    #глобальный стейт при завершении сценария после отправки заявки на тур
    state: End
        a: До свидания!
        script: $session = {}      //обнулили переменные сессии


#============================================ ЗАПРОС ИМЕНИ ===============================================================

theme: /Name

    #запрашиваем имя, любые слова считаем именем, поэтому остаемся в этом стейте
    state: AskName || modal = true
        a: Как я могу к Вам обращаться?
   
        #имя совпало с переменной из списка - сохраняем его и идем в Меню
        state: GetName
            q: * $Name *
            script: $client.name = $parseTree._Name.name;
            a: {{$client.name}}, приятно познакомиться!    
            go!: /Menu/Begin
        #не хочу знакомиться - соглашаемся и идем в меню
        state: NoName
            q: (* никак */* не надо */ * не хочу *)
            a: Как вам будет удобно. Обойдемся без знакомства.
            go!: /Menu/Begin
            
        #другое непонятное слово - уточняем имя это или нет
        state: GetStrangeName
            q: * 
            script: $temp.Name = $request.query;
            a: {{$temp.Name}}! Какое необычное имя. Вы не ошиблись? Я могу вас так называть?
            buttons:
                "Да"
                "Нет"
            #если имя - сохраняем его и идем в Меню
            state: Yes
                q: (да/* верно *)
                script: $client.name = $temp.Name;
                a: {{$client.name}}, приятно познакомиться!
                go!: /Menu/Begin
            #если не имя - соглашаемся не знакомиться и идем в Меню
            state: No
                q: (нет/* не верно *)
                a: Как вам будет удобно. Обойдемся без знакомства.
                go!: /Menu/Begin
                
#====================================================== МЕНЮ ===========================================================

theme: /Menu
    state: Begin
        a: Я расказываю о погоде в разных городах мира и могу оформить заявку на подбор тура. 
        go!: /Menu/Choose

    #спрашиваем что выбрает
    state: Choose
        a: Что Вас интересует?
# ДОБАВИТЬ ИНТЕНТЫ ПЕРЕДЕЛАТЬ КНОПКИ НА ЧЕТКИЕ ПЕРЕХОДЫ        
        #переход по кнопкам ведет в нужные стейты
        buttons:
            "Рассказать о погоде"
            "Оформить заявку на тур"
        #интент Что еще умеешь - идем в начало выбора       
        state: WhatElse
            q: * [что] еще [умеешь]*
            q: * другое *
            a: Я пока больше ничего не умею. Только рассказывать о погоде и оформлять заявку на тур.
            go!: /Menu/Choose
        #интент Отказ - идем на выход
        state: Deny
            q: (* ничего/отказ *)
            q: (* (не хочу)/(не надо) *)
            q: * [это] не то *
            a: Как скажете.
            go!: /Exit
        #интент город/страна 
        state: Location
            q: * $City *
            q: * $Country *
            #запоминаем город или страну и их координаты
            script: 
                if ($parseTree.City) {
                    $session.place = {name: $parseTree._City.name, namesc: "", type: "city"};
                    $session.coordinates = {lat: $parseTree._City.lat, lon: $parseTree._City.lon};
                }
                else {
                    $session.place = {name: $parseTree._Country.name, namesc: $parseTree._Country.namesc, type: ""};
                    $session.coordinates = {lat: $parseTree._Country.lat, lon: $parseTree._Country.lon};
                }
            #запрашиваем дату
            a: {{$session.place.name}}? Интересная идея! Сейчас узнаю какая там погода. Какую дату посмотреть?
            #дата названа - записываем дату и идем в прогноз
            state: Date
                q: * @duckling.date *
                script: $session.date = $parseTree.value;
                go!: /Weather/Begin
            #интент отказ - предлаагаем варианты и идем в меню
            state: Deny
                q: * (ничего/никакую) *
                q: (*  (не хочу)/(не надо) *)
                q: (* нет/не надо *)
                a: Хотите, оформим заявку на подбор тура? Или посмотрим погоду в другом месте.
                go!: /Menu/Choose
            #ответ непонятен - ругаемся и идем в меню
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
        #если уже есть город/страна
        if: $session.place
            #если это город - идем на запрос даты
            if: ($session.place.type == "city")
                go!: /Weather/Step2
            #иначе спрашиваем, будет ли город, идем на обработку этого вопроса
            else:
                a: Смотрю погоду в {{$session.place.namesc}}. Можете назвать город?
                go!: /Weather/AskCity
        #если нет ни города, ни страны - запрашиваем
        else:    
            a: Назовите город или страну
        #если назвали город - записали город и идем на запрос даты    
        state: City    
            q: * $City *
            script:
                $session.place = {name: $parseTree._City.name, namesc: "", type: "city"}
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
        #назван город - сохранили его и идем смотреть прогноз
        state: City
            q: * $City *
            script:
                $session.place = {name: $parseTree._City.name, namesc: "", type: "city"}
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
        #если дата уже была раньше сохранена - идем на Шаг3 погоды
        if: $session.date
            go!: /Weather/Step3
        #иначе - спрашиваем дату
        else: 
            a: На какую дату смотрим прогноз погоды?
        #названа дата - сохраняем ее и идем на Шаг3 погоды
        state: Date
            q: * @duckling.date *
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
        #если нет - уточняем у пользователя что он хочет
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
        
    #функция запроса погоды
    state: ForecastStep4
        script:
            #запрашиваем погоду по API и сохраняем температуру
            $temp.weather = getWeather($session.coordinates.lat, $session.coordinates.lon, $session.interval);
            $session.temperature = $temp.weather.temp;
        #если ответ пришел - выдаем его
        if: $temp.weather
            script: 
                #формируем часть ответа про день/дату, на который получен прогноз
                if ($session.interval == 0) $temp.answerDate = "сегодня";
                    else if ($session.interval == 1) $temp.answerDate = "завтра";
                    else {
                        $temp.answerDate = "";
                        $temp.answerDate += $session.date.day + "." + $session.date.month + "." + $session.date.year;
                    }
                #формируем часть ответа про место
                $temp.answerPlace = "";
                if ($session.place.type == "city") $temp.answerPlace += "городе " + $session.place.name
                    else $temp.answerPlace += $session.place.namesc;
            #выдаем полный ответ про погоду
            a: Погода в {{$temp.answerPlace}} на {{$temp.answerDate}}: {{($temp.weather.descript).toLowerCase()}}, {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
            
        #если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!: /Menu/Begin
        #идем спрашивать клиента про климат
        go!: /Weather/Step5
    
    #функция запроса исторических данных о погоде
    state: HistoryStep4
        script:
            #формируем для запроса входную и выходную дату в прошлом году
            $session.historyDay1 = "";
            $session.historyDay2 = "";
            $session.historyDay1 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + $session.date.day;
            $session.historyDay2 += minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + plus($session.date.day);
            #запрашиваем погоду  по API и сохраняем температуру
            $temp.weather = getHistoricalWeather($session.coordinates.lat, $session.coordinates.lon, $session.historyDay1, $session.historyDay2);
            $session.temperature = $temp.weather.temp;
        #если ответ пришел - выдаем его
        if: $temp.weather
            #формируем ответ про день/дату, на который получен прогноз            
            script: 
                $temp.answerDate = "";
                $temp.answerDate += $session.date.day + "." + $session.date.month; 
                #формируем часть ответа про место
                $temp.answerPlace = "";
                if ($session.place.type == "city") $temp.answerPlace += "городе " + $session.place.name
                    else $temp.answerPlace += $session.place.namesc;
            #выдаем полный ответ про погоду
            a: Погода в {{$temp.answerPlace}} в прошлом году на {{$temp.answerDate}} была: {{$temp.weather.temp}}°C. Ветер {{$temp.weather.wind}} м/с, порывы до {{$temp.weather.gust}} м/с.
        #если ответ не пришел - извиняемся и идем в главное меню
        else: 
            a: Запрос погоды не получен по техническим причинам. Пожалуйста, попробуйте позже
            go!: /Menu/Begin
        #идем спрашивать клиента про климат
        go!: /Weather/Step5

    state: Step5
        #уточняем: точно ли клиент хочет поехать в умеренный/холодный/теплый климат
        if: ($session.temperature < 25 && $session.temperature > 0)
            a: Вы действительно планируете поездку в страну с умеренным климатом?
        if: ($session.temperature < 0 || $session.temperature == 0)
            a: Вы действительно планируете поездку в страну с холодным климатом?
        if: ($session.temperature > 25 || $session.temperature == 25)
            a: Вы действительно планируете поездку в страну с жарким климатом?
        buttons:
            "Да, планирую"
            "Нет, не планирую"
        #введен город/страна - запомнили их и идем на старт погоды
        state: Location
            q: * $City *
            q: * $Country *
            script: 
                if ($parseTree.City) {
                    $session.place = {name: $parseTree._City.name, namesc: "", type: "city"};
                    $session.coordinates = {lat: $parseTree._City.lat, lon: $parseTree._City.lon};
                }
                else {
                    $session.place = {name: $parseTree._Country.name, namesc: $parseTree._Country.namesc, type: ""};
                    $session.coordinates = {lat: $parseTree._Country.lat, lon: $parseTree._Country.lon};
                }
            a: {{$session.place.name}}? Сейчас узнаю какая там погода.
            go!: /Weather/Begin
        #введена дата - запомнили её и идем на Шаг3 погоды
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value;
            go!: /Weather/Step3
        #ответ нет - идем на шаг6 погоды
        state: NoSure
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * не планирую *
            go!: /Weather/Step6
        #если да - предлагаем оформить заявку (или продолжить её оформление)
        state: YesSure
            q: * $comYes *
            q: (да/верно)
            q: планирую
            if: $session.tripStep
                a: Продолжим оформление заявки на тур?
            else: 
                if: ($session.place.type == "city") 
                    a: Давайте оформим заявку на тур в этот город?
                else:     
                    a: Давайте оформим заявку на тур в эту страну?
                    
            #если да - идем в раздел Заявка
            state: Yes
                q: * $comYes *
                q: (да/верно)
                q: * давайте *
                go!: Trip/Begin
            #если нет - идем на шаг6 погоды    
            state: Deny
            q: * $comNo *
            q: * (не верно/неверно) *
            q: * не планирую *
            go!: /Weather/Step6        
                    
    state: Step6                
        a: Давайте посмотрим климат в другом месте?
        buttons: 
            "Прогноз в другом месте"
            "Не нужен прогноз"
        #другое место очищаем место и дату, идем на начало прогноза
        state: ChangePlaceDate
            q: * другое место и дата *
            q: [~другой] (~место) (~дата)
            q: * $comYes *
            q: (да/давайте)
            script: 
                delete $session.place, 
                delete $session.coordinates, 
                delete $session.date
            go!: /Weather/Begin     
        #не нужен прогноз - идем на выход
        state: Deny
            q: * $comNo *
            q: * (не хочу/не надо) *
            q: * не нуж* *
            a: Как скажете!
            go!: /Exit
        #названа дата - запомнили её и идем на шаг3 прогноза

#============================================= ОФОРМЛЕНИЕ ПУТЕВКИ =============================================#

theme:/Trip
    
    state: begin
    #если заявку начинали оформлять то идем туда где остановились, иначе идем проверять/спрашивать имя
    if: $session.tripStep
        script: $reactions.transition ( {value:$session.tripStep, deferred: false} );
    else: 
        a: Для оформления заявки я задам вам несколько вопросов. Обязательными будут только Ваше имя и телефон
        if: $client.name
            go!: /Trip/CheckName
        else:
            go!: /Trip/AskName
    
    #если имя уже есть - проверяем актуально ли оно
    state: CheckName
        a: Ваше имя {{$client.name}}, верно?
        buttons:
            "Да"
            "Нет"
        #если да - идем на Шаг2 заявки   
        state: Yes
            q: (да/* верно *)
            a: Внесла в заявку
            go!: /Trip/Step2
        #если нет - идем спрашивать имя
        state: No
            q: * $comNo *
            q: * (не верно/неверно) *
            q: *[~другой]/[~иначе] *
            script: delete $client.name
            go!: /Trip/AskName
        #если отказ знакомиться - извиняемся и идем на выход
        state: Deny
            q: * отказ *
            q: (* (не хочу)/(не надо) *)
            q: * не буду *
            a: К сожалению, без имени я не могу принять заявку на тур
            go!: /Exit
        #если нет внятного ответа - возвращаемся к вопросу
        state: CatchAll || noContext = true 
            event: noMatch
            a: Пожалуйста, ответьте да или нет:
            go!: /Trip/CheckName
    
    #если имени нет - запрашиваем его
    state: AskName || modal = true
        a: Пожалуйста, назовите Ваше имя
        #имя совпало с переменной из списка - сохраняем имя, идем на Шаг2 заявки
        state: GetName
            q: * $Name *
            script: $client.name = $parseTree._Name.name;
            a: {{$client.name}}, приятно познакомиться!    
            go!: /Trip/Step2
        #не хочу знакомиться - извиняемся и идем на выход
        state: NoName
            q: (* не назову */* не хочу */* не буду */отказ)
            a: К сожалению, без имени я не могу принять заявку на тур
            go!: /Exit
        #другое непонятное слово - уточняем имя это или нет
        state: GetStrangeName
            q: * 
            script: $temp.Name = $request.query;
            a: {{$temp.Name}}! Какое необычное имя. Вы не ошиблись? Я могу вас так называть?
            buttons:
                "Да"
                "Нет"
            #если имя - сохраняем его и идем на Шаг2 заявки
            state: Yes
                q: (да/* верно *)
                script: $client.name = $temp.Name;
                a: {{$client.name}}, приятно познакомиться!
                go!: /Trip/Step2
            #если не имя - пробуем с начала
            state: No
                q: (нет/* не верно */ошиб* /* не може* *)
                a: Попробуем еще раз.
                go!: /Trip/AskName

    #если телефон есть, идем его проверять, иначе - идем его спрашивать
    state: Step2
        script: $session.tripStep = "/Trip/Step2"
        if: $client.phone
            go!: /Trip/CheckPhone
        else:
            go!: /Trip/AskPhone
        
    #проверяем актуален ли телефон
    state: CheckPhone
        a: Ваш телефон {{$client.phone}}, верно?
        buttons:
            "Да"
            "Нет"
        #если да - идем на Шаг3 заявки 
        state: Yes
            q: (да/* верно *)
            a: Внесла в заявку
            go!: /Trip/Step3
        #если нет - идем спрашивать телефон
        state: No
            q: * $comNo *
            q: * (не верно/неверно) *
            q: *[~другой]/[~иначе] *
            script: delete $client.phone
            go!: /Trip/AskPhone
        #если отказ - извиняемся и идем на выход
        state: Deny
            q: * отказ *
            q: (* (не хочу)/(не надо) *)
            q: * не буду *
            a: К сожалению, без номера телефона я не могу принять заявку
            go!: /Exit
        #если нет внятного ответа - возвращаемся к вопросу
        state: CatchAll || noContext = true 
            event: noMatch
            a: Пожалуйста, ответьте да или нет:
            go!: /Trip/CheckPhone
           
    #если телефона нет - запрашиваем его
    state: AskPhone
        a: Пожалуйста, назовите номер Вашего телефона
        #имя совпало с переменной из списка - сохраняем имя, идем на Шаг2 заявки
        state: GetPhone
            q: * $phone *
            script: client.phone = $parseTree._phone
            a: {{client.phone}} внесла в заявку
            go!: /Trip/Step3
        #не хочу знакомиться - извиняемся и идем на выход
        state: NoPhone
            q: (* не назову */* не хочу */* не буду */отказ)
            a: К сожалению, без номера телефона я не могу принять заявку
            go!: /Exit
        #иное - ругаемся и идем на начало
        state: NoMatch
            q: *
            a: Непохоже на номер телефона. Давайте попробуем еще раз.
            go!: /Trip/AskPhone
    #запрашиваем дату   
    state: Step3   
        script: $session.tripStep = "/Trip/Step3"
        a: Назовите дату начала поездки. Можно примерно
        buttons:
            "Пока не знаю"
        #введена дата - сохраняем ее
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value;
            go!: /Trip/Step3
        #введено что-то иное - сохраняем это в другой переменной
        state: NoDate
            q: *
            script: $session.noDate = $request.query;
            go!: /Trip/Step3
    
    #запрашиваем длительность поездки
    state: Step4 
        script: $session.tripStep = "/Trip/Step4"
        a: Выберите длительность поездки
        buttons:
            "до 7 дней"
            "7-13 дней"
            "14-20 дней"
            "21-29 дней"
            "Свыше месяца"
            "Пока не знаю"
        
        state: Answer
            q: *
            script: $session.duration = $request.query;
            go!: /Trip/Step5
            
            
            



#========================= Вывод данных для пользователя и возможность изменить их ==========================

            
theme: /FullData               
    
    state: Screen
#ОСТАВЛЯТЬ ЛИ ЭТУ ЧАСТЬ СКРИПТА??!  
#answer - в temp!
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
        
        state: Yes
            q: * (да/верно) *
            go!: /SendMail/Mail

        state: No
            q: * заполнить заново *
            script:
                # delete $session.StartPoint;
                # delete $session.departureCity;
                # delete $session.departureDate;
                # delete $session.arrivalPointForCity;
                # delete $session.arrivalPointForCountry;
                # delete $session.returnDate;
                # delete $session.people;
                # delete $session.children;
                # delete $session.bablo;
                # delete $session.stars;
                # delete $session.comments;
            # go!: /Trip/TripStartPoint

#============================================= Отправка формы на почту менеджеру =============================================#            
        
theme: /SendMail
    state: Mail
        a: Заявка сформирована, отправляю в компанию Just Tour...
        script:
#ЗАЧЕМ тут var?? ПЕРЕМЕСТИТЬ в temp?
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
            a: Заявка отправлена. Менеджер Just Tour свяжется с вами в ближайшее время
            go!: /End
        else:
            a: Упс. Ваша заявка не отправилась.Для подбора тура обратитесь в JustTour по телефону. Продиктовать?
#ДОБИТЬ СЦЕНАРИЙ ПО СХЕМЕ