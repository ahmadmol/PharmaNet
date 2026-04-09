package com.pharmalink.designsystem.components



import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.ColumnScope

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp

import com.pharmalink.designsystem.theme.dimens



@Composable

fun PharmaCard(

    modifier: Modifier = Modifier,

    containerColor: Color = MaterialTheme.colorScheme.surface,

    elevationDp: Float = 6f,

    content: @Composable ColumnScope.() -> Unit,

) {

    val d = MaterialTheme.dimens

    Card(

        modifier = modifier.fillMaxWidth(),

        shape = MaterialTheme.shapes.large,

        colors = CardDefaults.cardColors(containerColor = containerColor),

        elevation = CardDefaults.cardElevation(

            defaultElevation = elevationDp.dp,

            pressedElevation = elevationDp.dp,

            hoveredElevation = elevationDp.dp + 2.dp,

        ),

    ) {

        Column(

            modifier = Modifier.padding(d.cardPadding),

            content = content,

        )

    }

}


