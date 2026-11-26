package com.miguelaetxio.mia2f.data.course

/**
 * miCursos format v1 (see miCursos/README.md). Field names must
 * match the JSON exactly.
 * ---
 * Formato v1 de miCursos (ver miCursos/README.md). Los nombres de
 * campo deben coincidir exactamente con el JSON.
 */
data class CourseIndex(
    val schemaVersion: Int,
    val courses: List<CourseEntry>,
)

data class CourseEntry(
    val id: String,
    val language: String,
    val level: String,
    val title: String,
    val path: String,
    val version: Int,
)

data class Course(
    val schemaVersion: Int,
    val id: String,
    val language: String,
    val instructionLanguage: String,
    val level: String,
    val version: Int,
    val title: String,
    val sources: List<CourseSource>,
    val units: List<CourseUnit>,
)

data class CourseSource(
    val id: String,
    val title: String,
    val url: String? = null,
)

data class CourseUnit(
    val id: String,
    val number: Int,
    val title: String,
    val canDo: List<CanDo>,
    val concepts: List<Concept>,
)

data class CanDo(
    val text: String,
    val sourceId: String,
)

data class Concept(
    val id: String,
    val type: String,
    val title: String,
    val explanation: String,
    val examples: List<String>,
    val sourceIds: List<String>,
)
