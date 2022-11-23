patterns:
    $hi = (привет*/здравствуй*)
    $phone = $regexp<((8|\+7|)[\- ]?)?\(?\d{3}\)?[\- ]?\d{1}[\- ]?\d{1}[\- ]?\d{1}[\- ]?\d{1}[\- ]?\d{1}(([\- ]?\d{1})?[\- ]?\d{1})>
    $City = $entity<cities> || converter = function(parseTree) {var id = parseTree.cities[0].value; return cities[id].value;};
    $Country = $entity<countries> || converter = function(parseTree) {var id = parseTree.countries[0].value; return countries[id].value;};
    $Name = $entity<names> || converter = function(parseTree) {var id = parseTree.names[0].value; return names[id].value;};
    $Question = (какой|какая|что с|че с|че|что|как|подскажи)
    $Weather = (~погода|~прогноз)
    $comYes = ({да [конечно]}/ага/так точно/конечно/естесственно/а как же/да/даа/дааа*/дада/дадада/lf/rjytxyj/fuf/tcntccndtyyj/da/ok)
