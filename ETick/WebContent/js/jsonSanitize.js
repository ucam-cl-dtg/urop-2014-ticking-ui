function toJSONString (string)
{
    return string.replace(/\n/gm, "\\n")
                 .replace(/\'/gm, "\\'")
                 .replace(/\"/gm, '\\"')
                 .replace(/\&/gm, "\\&")
                 .replace(/\r/gm, "\\r")
                 .replace(/\t/gm, "\\t")
                 .replace(/[\b]/gm, "\\b")
                 .replace(/\f/gm, "\\f");
}
