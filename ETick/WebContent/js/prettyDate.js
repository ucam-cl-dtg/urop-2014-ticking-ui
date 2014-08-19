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
        date.getUTCFullYear();
}

function prettyTime(time)
{
    return time.getUTCHours() + ":" +
        time.getUTCMinutes();
}

function prettyDateTime (datetime)
{
    return prettyDate(datetime) + " " +
        prettyTime(datetime);
}
