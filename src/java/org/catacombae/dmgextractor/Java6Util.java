/*-
 * Copyright (C) 2014 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catacombae.dmgextractor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class should encapsulate all of the logic in DMGExtractor that is
 * Java 6-specific.
 *
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class Java6Util extends org.catacombae.util.Java6Util {
    /**
     * Read password from console without echoing characters.
     *
     * @return <code>null</code> if there is no console or end of stream is
     * reached when attempting to read password.
     */
    public static char[] readPassword() {
        try {
            final Method systemConsoleMethod =
                    java.lang.System.class.getMethod("console");
            final Object consoleObject = systemConsoleMethod.invoke(null);
            if(consoleObject == null) {
                /* No console. */
                return null;
            }

            final Class<? extends Object> consoleClass =
                Class.forName("java.io.Console");
            final Method consoleReadPasswordMethod =
                    consoleClass.getMethod("readPassword");

            final Object passwordObject =
                    consoleReadPasswordMethod.invoke(consoleObject);
            if(passwordObject != null && !(passwordObject instanceof char[])) {
                throw new RuntimeException("Unexpected type returned from " +
                        "java.io.Console.readPassword(): " +
                        passwordObject.getClass().getName());
            }

            return (char[]) passwordObject;
        } catch(ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch(NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
