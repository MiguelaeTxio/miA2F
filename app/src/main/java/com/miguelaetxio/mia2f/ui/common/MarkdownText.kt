package com.miguelaetxio.mia2f.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/**
 * Renders Gemini's Markdown answers with Material 3 styling. Single
 * wrapper so the renderer can be swapped in one place.
 * ---
 * Renderiza las respuestas en Markdown de Gemini con estilo Material 3.
 * Envoltorio único para poder cambiar de renderizador en un solo sitio.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = markdown,
        modifier = modifier,
    )
}
