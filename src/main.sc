require: requirements.sc

#========================================= СТАРТ И ГЛОБАЛЬНЫЕ ИНТЕНТЫ ==================================================
theme: /  

    #старт диалога с интентов start и приветствия
    state: Welcome
        q!: *start                                  
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

    #глобальный стейт отказа или отмены
    state: Cancel
        q!: ( (* отмен*)/(* отказ*)/(* выход*)/(* стоп*) )
        a: Поняла, выхожу из диалога.
        go!: /Exit
    
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

    #предложили выбор; ответ про погоду поймает входной интент Погоды
    state: Choose
        a: Что Вас интересует?
        buttons:
            "Рассказать о погоде"
            "Оформить заявку на тур"
        
        #интент Что еще умеешь - идем в начало выбора       
        state: WhatElse
            q: * [что] еще [умеешь]*
            q: * другое *
            a: Я больше ничего не умею. Только рассказывать о погоде и оформлять заявку на тур.
            go!: /Menu/Choose
        #интент Отказ - идем на выход
        state: Deny
            q: (* ничего/отказ *)
            q: (* (не хочу)/(не надо) *)
            q: * [это] не то *
            a: Как скажете.
            go!: /Exit
            
        #интент Оформить заявку
        state: Tour
            q: ( * тур/путешествие/путевк * )
            q: ( (* заявк* *) / (* оформ* *) )
            a: В какой город или страну хотите поехать?
            buttons:
                "Нужна консультация"
                "Еще не решил куда"
            #назван город или страна
            state: CityOrCountry
                q:  * $City * 
                q:  * $Country * 
                #запоминаем город или страну и их координаты и идем на начало Заявки
                script: getLocation ($parseTree)  
                a: Записала, {{$session.place.name}}
                go!: /Trip/Begin
            #не решил или нужна консультация - идем на начало Заявки
            state: NoSure
                q: * потом *
                q: * консультаци* *
                q: ( (* не решил* *)/(*  не зна* *) )
                q: (* (не выбрал)/(не определил) *)
                a: Не проблема. Заполним заявку, а менеджер поможет Вам выбрать направление
                go!: /Trip/Begin
            #все остальные ответы    
            state: NoMatch
                event: noMatch
                a: Я Вас не поняла. Давайте заполним заявку, а направление выберете потом
                go!: /Trip/Begin    
                
        #интент город/страна, в него можно попасть только из стейта /Menu/Choose
        state: Location 
            q: * $City * || fromState = "/Menu/Choose", onlyThisState = true
            q: * $Country * || fromState = "/Menu/Choose", onlyThisState = true
            #запоминаем город или страну и их координаты
            script: getLocation ($parseTree)
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
            q: * (ничего/никакую) *
            q: ( (* не хочу *)/(* не надо *) )
            q: ( (* нигде *)/(* ни где *) )
            q: * отказ*
            a: Как скажете.
            go!: /Exit
        
        
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
            script: getLocation ($parseTree)
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
                    else $temp.answerDate = $session.date.day + "." + $session.date.month + "." + $session.date.year;
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
                $temp.answerDate = $session.date.day + "." + $session.date.month; 
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
                q: ( (* давай*)/(* оформ*)/(* заявк*) )
                q: ( (* продолж*)/(* стар*) )
                go!: /Trip/Begin
            #если нет - идем на шаг6 погоды    
            state: Deny
                q: * $comNo *
                q: * (не надо/не хочу) *
                go!: /Weather/Step6        
                    
    state: Step6                
        a: Давайте посмотрим климат в другом месте?
        buttons: 
            "Прогноз в другом месте"
            "Не нужен прогноз"
        #другое место очищаем место и дату, идем на начало прогноза
        state: ChangePlaceDate
            q: * (другое/другом место/месте) *
            q: [~другой] (~место) [~дата]
            q: * $comYes *
            q: (да/давай)
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

#============================================= ОФОРМЛЕНИЕ ПУТЕВКИ =============================================#

theme:/Trip
    
    state: Begin
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
            q: (* (~другой)/(~иначе) *)
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
            q: (* (~другой)/(~иначе) *)
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
            script: $client.phone = $parseTree._phone
            a: {{$client.phone}} внесла в заявку
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
            "Еще не знаю"
        #введена дата - сохраняем ее
        state: Date
            q: * @duckling.date *
            script: $session.date = $parseTree.value;
            go!: /Trip/Step4
        #введено что-то иное - сохраняем это в другой переменной
        state: NoDate
            q: *
            script: $session.noDate = $request.query;
            go!: /Trip/Step4
    
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
            "Еще не знаю"
        #подойдет любой ответ - записали его
        state: Answer
            q: *
            script: $session.duration = $request.query;
            go!: /Trip/Step5

    #запрашиваем количество участников поездки
    state: Step5 
        script: $session.tripStep = "/Trip/Step5"
        a: Сколько всего человек будет в поездке включая детей?
        buttons:
            "Еще не знаю"
        # если введено число - записали его и пошли спросить про детей
        state: Number
            q: * @duckling.number * || fromState = "/Trip/Step5", onlyThisState = true
            script: $session.people = $request.query;
            go!: /Trip/Step5/Children
        # спросили про детей 
        state: Children
            a: Сколько из них детей младше 14 лет?
            #подойдет любой ответ - записали его и идем на Шаг6 заявки
            state: Answer
                q: *
                script: $session.children = $request.query;
                go!: /Trip/Step6
        # если любой другой ответ - записали его как есть и идем на Шаг6 заявки
        state: Answer
            event: noMatch || fromState = "/Trip/Step5", onlyThisState = true
            script: $session.people = $request.query;
            go!: /Trip/Step6
            
    #запрашиваем бюджет поездки
    state: Step6 
        script: $session.tripStep = "/Trip/Step6"
        a: Какой примерно бюджет поездки из расчета на одного взрослого?
        buttons:
            "до $300"        
            "$300-$700"        
            "$700-$1500"        
            "$1500-$3000"
            "свыше $3000"
            "Еще не знаю"
        #подойдет любой ответ - записали его
        state: Answer
            q: *
            script: $session.budget = $request.query;
            go!: /Trip/Step7

    #запрашиваем бюджет поездки
    state: Step7 
        script: $session.tripStep = "/Trip/Step7"
        a: Какой хотите минимальный уровень звездности отеля?
        buttons:
            "не важно"        
            "3*"        
            "4*"        
            "5*"
            "Еще не знаю"
        #подойдет любой ответ - записали его
        state: Answer
            q: *
            script: $session.stars = $request.query;
            go!: /Trip/Step8

    #запрашиваем бюджет поездки
    state: Step8 
        script: $session.tripStep = "/Trip/Step8"
        a: Что-то еще передать менеджеру? Может, какие-то пожелания?
        #подойдет любой ответ - записали его
        state: Answer
            q: *
            script: $session.comment = $request.query;
            go!: /SendMail/Mail

#============================================= Отправка формы на почту менеджеру =============================================#            
        
theme: /SendMail
    state: Mail
        a: Заявка сформирована, отправляю в компанию Just Tour...
        #формируем и отправляем заявку
        script: $session.result = email ()
        #если результат ОК - сообщаем об этом
        if: ($session.result.status == "OK")
            a: Заявка отправлена. Менеджер Just Tour свяжется с вами в ближайшее время
            go!: /End
        #если результат не ОК - тоже сообщаем об этом и идем ловить ответ
        else:
            a: Упс. Ваша заявка не отправилась. Для подбора тура обратитесь в JustTour по телефону. Продиктовать?
        #если согласие диктуем и спрашиваем надо ли повторить?
        state: Phone
            q: * $comYes *
            q: (диктуй/диктовать)
            q: * давай *
            a: Телефон компании Just Tour 8-812-000-0000. Повторить?
            #если надо повторить - идем на шаг назад
            state: Yes
                q: * $comYes *
                q: (повтори/диктуй)
                q: * давай *
                go!: /SendMail/Mail/Phone
        #во всех других случаях - идем на выход прощаться
        state: NoMatch
            event: noMatch
            go!: /Exit