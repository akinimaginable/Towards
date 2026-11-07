package org.etrange.towards

import androidx.compose.ui.window.ComposeUIViewController
import org.etrange.towards.data.IosLocationProvider

private val graph by lazy { AppGraph(IosLocationProvider()) }

fun MainViewController() = ComposeUIViewController { App(graph) }