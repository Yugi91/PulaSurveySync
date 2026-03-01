package com.pula.survey.sync.testdata

import com.pula.survey.sync.domain.model.Attachment
import com.pula.survey.sync.domain.model.SurveyAnswer
import com.pula.survey.sync.domain.model.SurveyResponse
import com.pula.survey.sync.domain.model.SyncStatus
import com.pula.survey.sync.domain.repository.SurveyRepository
import java.util.UUID
import kotlin.random.Random

class TestDataGenerator(private val repository: SurveyRepository) {

    private val firstNames = listOf(
        "John", "Mary", "Peter", "Grace", "James", "Sarah", "David", "Faith",
        "Joseph", "Esther", "Samuel", "Ruth", "Daniel", "Mercy", "Michael"
    )
    private val lastNames = listOf(
        "Ochieng", "Wanjiku", "Kamau", "Muthoni", "Otieno", "Akinyi", "Njoroge",
        "Wambui", "Kipchoge", "Chebet", "Ouma", "Adhiambo", "Mwangi", "Nyambura"
    )
    private val crops = listOf("Maize", "Sorghum", "Millet", "Rice", "Wheat", "Cassava", "Sweet Potato", "Beans")
    private val surveyNames = listOf("Crop Yield Assessment", "Soil Health Survey", "Pest Impact Evaluation", "Harvest Report")

    suspend fun generate(count: Int, withAttachments: Boolean = true) {
        repeat(count) {
            val responseId = UUID.randomUUID()
            val farmerName = "${firstNames.random()} ${lastNames.random()}"
            val farmCount = Random.nextInt(1, 5)

            val answers = buildAnswers(farmerName, farmCount)
            val now = System.currentTimeMillis() - Random.nextLong(0, 3600_000)

            val response = SurveyResponse(
                id = responseId,
                farmerId = farmerName,
                surveyId = surveyNames.random(),
                answers = answers,
                attachments = emptyList(),
                status = SyncStatus.PENDING,
                createdAt = now,
                updatedAt = now,
                retryCount = 0,
                lastErrorType = null
            )

            val attachments = if (withAttachments) {
                val photoCount = Random.nextInt(0, 4)
                (1..photoCount).map { i ->
                    Attachment(
                        id = UUID.randomUUID(),
                        responseId = responseId,
                        filePath = "/storage/emulated/0/DCIM/survey_${responseId}_$i.jpg",
                        fileName = "field_photo_$i.jpg",
                        mimeType = "image/jpeg",
                        sizeBytes = Random.nextLong(500_000, 3_000_000),
                        createdAt = now
                    )
                }
            } else emptyList()

            repository.saveResponse(response, attachments)
        }
    }

    private fun buildAnswers(farmerName: String, farmCount: Int): List<SurveyAnswer> {
        val baseAnswers = mutableListOf<SurveyAnswer>(
            SurveyAnswer.Single("farmer_name", farmerName),
            SurveyAnswer.Single("num_farms", farmCount.toString())
        )

        val farmRepetitions = (1..farmCount).map { farmIndex ->
            val lat = -1.0 + Random.nextDouble(-0.5, 0.5)
            val lon = 36.8 + Random.nextDouble(-0.5, 0.5)
            listOf(
                SurveyAnswer.Single("crop_type", crops.random()),
                SurveyAnswer.Single("area_hectares", String.format("%.1f", Random.nextDouble(0.5, 10.0))),
                SurveyAnswer.Single("yield_estimate_kg", Random.nextInt(200, 5000).toString()),
                SurveyAnswer.Single("gps_coordinates", String.format("%.3f, %.3f", lat, lon))
            )
        }

        baseAnswers.add(
            SurveyAnswer.RepeatingSection(
                sectionId = "farms",
                triggerQuestionId = "num_farms",
                repetitions = farmRepetitions
            )
        )

        return baseAnswers
    }
}
