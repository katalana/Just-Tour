# запускаемые модули/библиотеки
require: slotfilling/slotFilling.sc
    module = sys.zb-common
  
require: dateTime/moment.min.js
    module = sys.zb-common 

# библиотека локальных функций
require: func.js

# файл сценария с паттернами локальных переменных
require: localPatterns.sc  

# справочник городов
require: dicts/cities.csv
    name = cities
    var = cities

# справочник стран
require: dicts/countries.csv
    name = countries
    var = countries

# справочник имен
require: dicts/names.csv
    name = names
    var = names
