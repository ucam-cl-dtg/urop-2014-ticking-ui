function prettyDate(date)
{
    var days = ["Sun", "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat"];

    var months = ["January", "February", "March",
                  "April", "May", "June",
                  "July", "August", "September",
                  "October", "November", "December"];

    return days[date.getUTCDay()] + ", " +
        date.getUTCDate() +
        months[date.getUTCMonth()]  +
        date.getUTCFullYear();
}

function prettyTime(time)
{
    return date.getUTCHours() + ":" +
        date.getUTCMinutes();
}

function prettyDateTime (date)
{
    return prettyDate(date) + " " +
        prettyTime(time);
}
