/**
 * Copyright (c) 2012 Committers of lunifera.org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Florian Pirchner - initial API and implementation
 */
package org.lunifera.runtime.common.keystroke;

/**
 * A callback to handle key strokes.
 */
public interface KeyStrokeCallback {

	/**
	 * Is called if the keystroke definition was activated.
	 * 
	 * @param source
	 * @param target
	 */
	void callback(Object source, Object target);

}
