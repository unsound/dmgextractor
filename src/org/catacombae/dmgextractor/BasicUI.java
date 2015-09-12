/*-
 * Copyright (C) 2008 Erik Larsson
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

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
abstract class BasicUI implements UserInterface {

    protected long totalProgressLength = 0;
    protected long currentProgress = 0;
    /** Used to prevent unneccessary updates of the progress meter. */
    protected long previousPercentage = -1;
    protected final boolean verbose;

    public BasicUI(boolean verbose) {
        this.verbose = verbose;
    }

    /** {@inheritDoc} */
    public void setTotalProgressLength(long len) {
        totalProgressLength = len;
    }

    /** {@inheritDoc} */
    public void addProgressRaw(long value) {
        currentProgress += value;
        if(totalProgressLength > 0) {
            reportProgress((int) (currentProgress * 100 / totalProgressLength));
        }
        else {
            reportProgress(0);
        }
    }

    /** {@inheritDoc} */
    public void displayMessageVerbose(String... messageLines) {
        if(verbose)
            displayMessage(messageLines);
    }
}
