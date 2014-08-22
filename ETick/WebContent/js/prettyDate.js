/* Wrappers, so if we decide to change to UTC, etc. we can do it here. */
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
    return moment(dateString, moment.ISO_8601);
}

function prettyDate(date)
{
    var days = ["Sun", "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat"];

    var months = ["January", "February", "March",
                  "April", "May", "June",
                  "July", "August", "September",
                  "October", "November", "December"];

    return days[date.day()] + ", " +
        date.date() + " " +
        months[date.month()] + " " +
        padLeft(date.year(), 4, "0");
}

function prettyTime(time)
{
    return padLeft(time.hours(), 2, "0") + ":" +
           padLeft(time.minutes(), 2, "0");
}

function prettyDateTime (datetime)
{
    return prettyDate(datetime) + " " +
        prettyTime(datetime);
}

function prettyGetDate (date)
{
    return date.date();
}
function prettySetDate (date, value)
{
    date.date(value);
}

function prettyGetMonth (date)
{
    return date.month();
}
function prettySetMonth (date, value)
{
    date.month(value);
}

function prettyGetFullYear (date)
{
    return date.year();
}
function prettySetFullYear (date, value)
{
    date.year(value);
}

function prettyGetHours (date)
{
    return date.hours();
}
function prettySetHours (date, value)
{
    date.hours(value);
}

function prettyGetMinutes (date)
{
    return date.minutes();
}
function prettySetMinutes (date, value)
{
    date.minutes(value);
}

function prettyGetDay (date)
{
    return date.day();
}
