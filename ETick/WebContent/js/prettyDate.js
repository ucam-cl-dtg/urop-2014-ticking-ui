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

    return days[date.getDay()] + ", " +
        date.getDate() + " " +
        months[date.getMonth()] + " " +
        padLeft(date.getFullYear(), 4, "0");
}

function prettyTime(time)
{
    return padLeft(time.getHours(), 2, "0") + ":" +
           padLeft(time.getMinutes(), 2, "0");
}

function prettyDateTime (datetime)
{
    return prettyDate(datetime) + " " +
        prettyTime(datetime);
}

function prettyGetDate (date)
{
    return date.getDate();
}
function prettySetDate (date, value)
{
    date.setDate(value);
}

function prettyGetMonth (date)
{
    return date.getMonth();
}
function prettySetMonth (date, value)
{
    date.setMonth(value);
}

function prettyGetFullYear (date)
{
    return date.getFullYear();
}
function prettySetFullYear (date, value)
{
    date.setFullYear(value);
}

function prettyGetHours (date)
{
    return date.getHours();
}
function prettySetHours (date, value)
{
    date.setHours(value);
}

function prettyGetMinutes (date)
{
    return date.getMinutes();
}
function prettySetMinutes (date, value)
{
    date.setMinutes(value);
}

function prettyGetDay (date)
{
    return date.getDay();
}
