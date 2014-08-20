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
