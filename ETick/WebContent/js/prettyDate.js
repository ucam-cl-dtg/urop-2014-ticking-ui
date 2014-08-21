function padLeft(input, width, padChar)
{
    if (typeof input != typeof "")
        input = input.toString();

    while (input.length < width)
    {
        input = padChar.toString() +
                  input.toString();
    }
    return input;
}

function prettyParse(dateString)
{
    if (typeof dateString != typeof "")
        return dateString; /* In case we get a Date passed */

    var match = dateString.match(/\d{4}-\d\d-\d\d(T\d\d:\d\d:\d\d(.\d{3})?)?/);

    if (match.length === 0)
    {
        throw "Invalid date format, expected something" +
           " like 2000-12-25T09:00:00.000, got " + dateString;
    }
    else
    {
        return new Date(match[0] + "Z");
    }
}

function prettyDate(date)
{
    var days = ["Sun", "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat"];

    var months = ["January", "February", "March",
                  "April", "May", "June",
                  "July", "August", "September",
                  "October", "November", "December"];

    return days[date.getUTCDay()] + ", " +
        date.getUTCDate() + " " +
        months[date.getUTCMonth()] + " " +
        padLeft(date.getUTCFullYear(), 4, "0");
}

function prettyTime(time)
{
    return padLeft(time.getUTCHours(), 2, "0") + ":" +
           padLeft(time.getUTCMinutes(), 2, "0");
}

function prettyDateTime (datetime)
{
    return prettyDate(datetime) + " " +
        prettyTime(datetime);
}
