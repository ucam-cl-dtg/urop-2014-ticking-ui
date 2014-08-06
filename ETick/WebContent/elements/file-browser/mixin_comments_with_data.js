/* vim: set shiftwidth=2 tabstop=2 softtabstop=2 expandtab textwidth=72 spell spelllang=en_gb : */
/**
 * This function mixes in comments with data, on a character basis. The
 * result is similar to the following snippet. This should handle
 * improper nesting (i.e. one comment from 1-3 and another from 2-4).
 * To use with HighlightJS, use the spans_to_comments function the
 * output of HighlightJS and concatenate that to the comments parameter.
 *
 * Arguments:
 * [CODE]
 * <p>Hello, world.</p>
 * [/CODE]
 *
 * [DATA]
 * [ {   "start"   : 3
 *     , "end"     : 8
 *     , "message" : "Hello is a greeting"
 *     , "class"   : "message1"
 *   }
 * , {   "start"   : 3
 *     , "end"     : 15
 *     , "message" : "This is a standard result."
 *     , "class"   : "helloWorld"
 * } ]
 * [/DATA]
 *
 * Result:
 * [HTML]
 * <span>&lt;p&gt;</span><span class="message1 helloWorld">Hello, world\
 * </span><span class="message1">.&lt;/p&gt;</span>
 * [/HTML]
 *
 * This works in multiple stages.
 * 1) Sort comment by start number
 * 2) Process the data, replacing valid HTML characters, checking if a
 *    comment starts or ends here (this just peeks at the top of the
 *    inactive (comments not yet started) and active (comments that have
 *    already started) queues).
 */

/* TODO: https://developers.google.com/closure/ */
function mixin_comments_with_data(data, comments)
{
  /* Strict mode function */
  "use strict";

  if (typeof data == "undefined")
  {
    return undefined;
  }
  else if (typeof comments == "undefined")
  {
    comments = new Array();
  }

  var activeComments   = new Array();
  var rtn = "<ol><li><span>"; /* So that we can always have a closing tag */

  /* Stage 1 – Sort */
  comments.sort(function (a, b)
                {
                  return b.start - a.start;
                });

  /* Stage 2 – Process */
  var i;
  var len = data.length;
  for (i = 0; i < len; i++)
  {
    if (comments.length > 0
      &&comments[comments.length-1].start == i)
    {
      /* TODO: Heap data structure */

      while (comments.length > 0
           &&comments[comments.length-1].start == i)
      {
        activeComments.push(comments.pop());
      }

      activeComments.sort(function (a, b)
                          { /* Descending sort */
                            return b.end - a.end;
                          });

      rtn += "</span>";

      rtn += "<span class=\"";
      /* Assuming class does not contain " */
      rtn += activeComments
              .map(function (x) { return x.class; })
              .join(" ");
      rtn += "\">";
    }

    if (activeComments.length > 0
      &&activeComments[activeComments.length-1].end == i)
    {
      while (activeComments.length > 0
           &&activeComments[activeComments.length-1].end == i)
      {
        activeComments.pop();
      }

      rtn += "</span>";

      if (activeComments.length > 0)
      {
        rtn += "<span class=\"";
        /* Assuming class does not contain " */
        rtn += activeComments
                .map(function (x) { return x.class; })
                .join(" ");
        rtn += "\">";
      }
      else
      {
        rtn += "<span>";
      }
    }

    switch(data[i])
    {
      case '<':
        rtn += "&lt;";
        break;

      case '>':
        rtn += "&gt;";
        break;

      case '&':
        rtn += "&amp;";
        break;

      case '\n':
        rtn += "</span>";
        rtn += "</li><li>";
        if (activeComments.length > 0)
        {
          rtn += "<span class=\"";
          /* Assuming class does not contain " */
          rtn += activeComments
                  .map(function (x) { return x.class; })
                  .join(" ");
          rtn += "\">";
        }
        else
        {
          rtn += "<span>";
        }
        break;

      default:
        rtn += data[i];
    }
  }
  rtn += "</span></li></ol>";

  return rtn;
}

/**
 * This function is just a helper function to convert lines to start and
 * end characters. Note that line numbers start with 1, for conformance
 * with text editors (all of them I have encountered).
 *
 * @param data String()—the text (to infer line lengths).
 * @param comments Array({"line": ?, ...}, ...)—The data to convert
 * "line" to "start" and "end" numbers representing start and end
 * characters of the line. Note, that this gets modified too, due to
 * Objects being references, but don't rely on it being modified (e.g.
 * if comments is undefined). Also, "line" is not deleted because of
 * this.
 * @return The modified Array({"start": ?, "end": ?, ...}, ...) to use
 * with mixin_comments_with_data(data, comments) as the comments value.
 */
function lines_to_chars(data, comments)
{
  "use strict";
  if (typeof data     == "undefined"
    ||typeof comments == "undefined")
  {
    return [];
  }

  var lineStart = [0];
  var i;
  var len = data.length;
  for (i = 0; i < len; i++)
  {
    if (data[i] == '\n')
    {
      lineStart.push(i+1);
    }
  }

  /* Non-existent line after last line. */
  lineStart.push(i+1);

  return comments.map(function (x)
                      {
                        /* Assumes x.line is valid for data */
                        if (typeof x.linNumber != "undefined"
                          &&typeof x.line == "undefined")
                        {
                          x.start = lineStart[x.lineNumber-1];
                          x.end   = lineStart[x.lineNumber]-1;
                        }
                        else if (typeof x.line != "undefined")
                        {
                          x.start = lineStart[x.line-1];
                          x.end   = lineStart[x.line]-1;
                        }
                        return x;
                      });

}

/**
 * Due to the way mixin_comments_with_data works, we can not have other
 * spans in the data, so this extracts spans from data and converts them
 * into comments of the following form.
 * [ {   "start" : ? Where span starts
 *     , "end"   : ? Where span ends
 *     , "class" : ? Class(es) of the span
 * } ]
 *
 */
function spans_to_comments(data)
{
  var i, j;
  var stack = new Array(); /* For class tags */
  var rtn = new Array();
  var len = data.length;
  var tmp;

  /* i is text index, j is HTML index, e.g. for <b>foo</b> if i=1 then
   * j=4 */
  for (i = 0, j = 0; j < len; i++, j++)
  {
    switch (data[j])
    {
      case '<':
        /* HTML comment */
        if (data.substr(j+1, 3) == "!--")
        {
          /* Skips <!--...--> including '-->' due to `for` j++ */
          while (j < len && data.substr(j+1, 3) != "-->") j++;
          j += 2;
        }
        else /* HTML tag */
        {
          /* We know data[j] is `<` */
          j++
          /* Skips <...> including '>' due to `for` j++ */
          tmp = "";
          while (j < len && data[j] != '>') tmp += data[j++];

          if (tmp.substr(0, 5) == "/span")
          {
            tmp = stack.pop();
            tmp.end = i;
            rtn.push(tmp);
          }
          else
          {
            stack.push({"start":i, "class" : []});
            tmp = tmp.replace(/class="([^"]*)"/,
                      function (match, capture)
                      {
                        stack[stack.length-1].class =
                          stack[stack.length-1].class
                            .concat(capture);
                      });
            if (stack[stack.length-1].class.length > 0)
            {
              stack[stack.length-1].class =
                stack[stack.length-1].class.join(" ");
            }
            else
            {
              stack.pop();
            }
          }
        }
        /* We did not actually come across a character yet */
        i--;
        break;

      case '&':
        /* Skips &...; including ';' due to `continue` */
        while (j < len && data[j] != ';')
        {
          j++;
        }
        break;

      default:
        break;
    }
  }

  while (stack.length > 0)
  {
    rtn.push(stack.pop())
    rtn[rtn.length-1].end = i;
  }

  return rtn;
}
