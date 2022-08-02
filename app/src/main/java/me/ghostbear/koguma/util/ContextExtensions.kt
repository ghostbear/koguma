/*
 * Copyright (C) 2022 ghostbear
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package me.ghostbear.koguma.util

import android.content.Context
import android.widget.Toast

fun Context.toast(
    text: String,
    length: Int = Toast.LENGTH_SHORT
) {
    Toast.makeText(this, text, length).show()
}