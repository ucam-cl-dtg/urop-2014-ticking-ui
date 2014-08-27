function isHidden (file)
{
    if (!String.prototype.endsWith) {
        Object.defineProperty(String.prototype, 'endsWith', {
            value: function (searchString, position) {
                var subjectString = this.toString();
                if (position === undefined || position > subjectString.length) {
                    position = subjectString.length;
                }
                position -= searchString.length;
                var lastIndex = subjectString.indexOf(searchString, position);
                return lastIndex !== -1 && lastIndex === position;
            }
        });
    }

    if (!String.prototype.startsWith) {
        Object.defineProperty(String.prototype, 'startsWith', {
            enumerable: false,
            configurable: false,
            writable: false,
            value: function (searchString, position) {
                position = position || 0;
                return this.lastIndexOf(searchString, position) === position;
            }
        });
    }

    /* Get rid of directories if file path to get name */
    fileName = file.name.split("/").pop();

    return fileName.startsWith(".")
        || fileName.endsWith(".class");
}
