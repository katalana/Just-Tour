theme: /Weather 
    
    state: Begin
    #вопрос из любого места о погоде
        # q!: * (~погода) *
        # q!: * (~прогноз) *
        intent!: /Погода
        #если в запросе найдена сущность Дата - сохраняем ее в сессионную переменную
        if: $parseTree.Date
            script: $session.date = $parseTree.Date[0].value
        #если в запросе найдена сущность Место - сверяем со справочниками Города и Страны
        if: $parseTree.Geo
            script: 
                $temp.geo = $parseTree.Geo[0].value;
                if ( $nlp.matchPatterns($temp.geo, ["$City"]) )
                    $session.geo = $nlp.matchPatterns($temp.geo, ["$City"]).parseTree
                else 
                    if ( $nlp.matchPatterns($temp.geo, ["$Country"]) )
                        $session.geo = $nlp.matchPatterns($temp.geo, ["$Country"]).parseTree
                    else $session.geo = ""
                #если Место совпало с данными справочников - сохраняем название и координаты
                if ( $session.geo ) getLocation ($session.geo)
        #если город/страна уже записаны в сессионные переменные
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
            a: Где смотрим погоду? Назовите город или страну
        #если назвали город - записали город и идем на Шаг 2 заявки   
        state: City    
            q: * $City *
            script: getLocation ($parseTree)
            go!: /Weather/Step2
        #если назвали страну - записали страну и идем на начало чтоб спросить про город
        state: Contry
            q: * $Country *
            script: getLocation ($parseTree)
            go!: /Weather/Begin
        #если отказ
        state: Deny
            intent: /Отказ
            a: Как скажете.
            go!: /Exit
        #если название непонятное - переспрашиваем и идем на начало чтоб спросить страну или город
        state: NoMatch
            event: noMatch
            a: Простите, я не знаю такого названия
            go!: /Weather/Begin

    #обработка запроса города если есть только страна
    state: AskCity
        #назван город - сохранили его и идем смотреть прогноз
        state: City
            q: * $City *
            script: getLocation ($parseTree)
            go!: /Weather/Step2        
        #ответ тупо "да" - просим назвать город, идем на начало обработки запроса
        state: Yes
            intent: /Да
            a: Назовите же его скорее!
            go!: /Weather/AskCity
        #ответ нет - идем смотреть прогноз по стране
        state: No
            intent: /Нет
            intent: /Незнаю
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
            intent: /Отказ
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
            script: $temp.answerDate = $session.date.day + " " + months[$session.date.month].name; 
            if: ( $session.interval == 17 ||  $session.interval > 17 )
                a: Не могу посмотреть прогноз больше, чем на 16 дней. Узнать какая погода была год назад {{$temp.answerDate}}?
            else:    
                a: Эта дата в прошлом. Узнать какая погода была {{$temp.answerDate}} в прошлом году?
        #если да - идем запрашивать исторические данные
        state: Yes
            intent: /Да
            q: (* узнай/посмотри *)
            go!: /Weather/HistoryStep4
        #если нет - очистили дату и идем снова спрашивать дату
        state: No
            intent: /Нет
            q: (* другая/другую [~дата] *)
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
                    else $temp.answerDate = $session.date.day + " " + months[$session.date.month].name + " " + $session.date.year;
                #формируем часть ответа про место
                if ($session.place.type == "city") $temp.answerPlace = "городе " + $session.place.name
                    else $temp.answerPlace = $session.place.namesc;
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
            $temp.historyDay1 = minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + $session.date.day;
            $temp.historyDay2 = minus($jsapi.dateForZone("Europe/Moscow","yyyy")) + "-" + $session.date.month + "-" + plus($session.date.day);
            #запрашиваем погоду  по API и сохраняем температуру
            $temp.weather = getHistoricalWeather($session.coordinates.lat, $session.coordinates.lon, $temp.historyDay1, $temp.historyDay2);
            $session.temperature = $temp.weather.temp;
        #если ответ пришел - выдаем его
        if: $temp.weather
            script: 
                #формируем часть ответа про дату, на который получен прогноз
                $temp.answerDate = $session.date.day + " " + months[$session.date.month].name; 
                #формируем часть ответа про место
                if ($session.place.type == "city") $temp.answerPlace = "городе " + $session.place.name
                    else $temp.answerPlace = $session.place.namesc;
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
            script: getLocation ($parseTree)
            a: {{$session.place.name}}? Сейчас узнаю какая там погода.
            go!: /Weather/Begin
        #введена дата - запомнили её и идем на Шаг3 погоды
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value;
            go!: /Weather/Step3
        #ответ нет - идем на шаг6 погоды
        state: NoSure
            intent: /Нет
            q: * не планир* *
            go!: /Weather/Step6
        #если да - предлагаем оформить заявку (или продолжить её оформление)
        state: YesSure
            intent: /Да
            q: планирую *
            if: $session.tripStep
                a: Продолжим оформление заявки на тур?
            else: 
                if: ($session.place.type == "city") 
                    a: Давайте оформим заявку на тур в этот город?
                else:     
                    a: Давайте оформим заявку на тур в эту страну?
                    
            #если да - идем в раздел Заявка
            state: Yes
                intent: /Да
                intent: /Тур
                q: * продолж*
                go!: /Trip/Begin
            #если нет - идем на шаг6 погоды    
            state: Deny
                intent: /Нет
                intent: /Отказ
                go!: /Weather/Step6        
                    
    state: Step6                
        a: Давайте посмотрим климат в другом месте?
        buttons: 
            "Прогноз в другом месте"
            "Не нужен прогноз"
        #другое место очищаем место и дату, идем на начало прогноза
        state: ChangePlaceDate
            q: * ~другой [~место] *
            q: * ~другой ~дата *
            intent: /Да
            script: 
                delete $session.place, 
                delete $session.coordinates, 
                delete $session.date
            go!: /Weather/Begin     
        #не нужен прогноз - идем на выход
        state: Deny
            intent: /Нет
            intent: /Отказ
            a: Как скажете!
            go!: /Exit
            
