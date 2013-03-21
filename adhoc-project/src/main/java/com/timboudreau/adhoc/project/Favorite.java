/* Copyright (C) 2013 Tim Boudreau

 Permission is hereby granted, free of charge, to any person obtaining a copy 
 of this software and associated documentation files (the "Software"), to 
 deal in the Software without restriction, including without limitation the 
 rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 sell copies of the Software, and to permit persons to whom the Software is 
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package com.timboudreau.adhoc.project;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class Favorite implements Comparable<Favorite> {

    private static final Pattern PAT = Pattern.compile("(-?\\d+):(.*)$");
    int count;
    final String relPath;

    static Favorite create(String data) {
        Matcher m = PAT.matcher(data);
        if (m.find()) {
            try {
                int pos = Integer.parseInt(m.group(1));
                String pth = m.group(2);
                return new Favorite(pos, pth);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String path() {
        return relPath;
    }

    public Favorite(int count, String relPath) {
        Parameters.notNull("relPath", relPath);
        this.count = count;
        this.relPath = relPath;
    }

    @Override
    public int compareTo(Favorite t) {
        Integer a = count;
        Integer b = t.count;
        return b.compareTo(a);
    }

    public boolean equals(Object o) {
        return o instanceof Favorite && ((Favorite) o).relPath.equals(relPath);
    }

    public int hashCode() {
        return relPath.hashCode();
    }

    public String toString() {
        return count + ":" + relPath;
    }
}
