package uzair.lightpods.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uzair.lightpods.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme
                            .typography.headlineMedium
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            painter = painterResource(
                                android.R.drawable
                                    .ic_menu_revert
                            ),
                            contentDescription = "Back",
                            modifier =
                                Modifier.size(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults
                    .largeTopAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme
                                .background,
                        scrolledContainerColor =
                            MaterialTheme.colorScheme
                                .surface
                    )
            )
        },
        containerColor =
            MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            item { AppInfoCard() }
            item { DeveloperCard(context) }
            item {
                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(
                    R.drawable.case_open_buds
                ),
                contentDescription = "LightPods",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LightPods",
                style = MaterialTheme
                    .typography.headlineSmall
                    .copy(
                        fontWeight = FontWeight.Bold
                    ),
                color = MaterialTheme
                    .colorScheme.onSurface
            )

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme
                    .typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "A Material You companion app " +
                    "for AirPods Pro clones. " +
                    "Detects battery levels, " +
                    "microphone location, ear " +
                    "detection, and case lid state " +
                    "via BLE proximity scanning.",
                style = MaterialTheme
                    .typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeveloperCard(
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Developer",
                style = MaterialTheme
                    .typography.titleMedium
                    .copy(
                        fontWeight =
                            FontWeight.SemiBold
                    ),
                color = MaterialTheme
                    .colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Uzair Mughal",
                style = MaterialTheme
                    .typography.titleLarge
                    .copy(
                        fontWeight = FontWeight.Bold
                    ),
                color = MaterialTheme
                    .colorScheme.onSurface
            )

            Text(
                text = "Full Stack Developer · " +
                    "Open Source Contributor " +
                    "(Linux Kernel) · " +
                    "Penetration Tester",
                style = MaterialTheme
                    .typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Muzaffarabad, " +
                    "Azad Kashmir, Pakistan",
                style = MaterialTheme
                    .typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Next.js · Firebase · " +
                    "Kotlin · Jetpack Compose · " +
                    "Python · Linux",
                style = MaterialTheme
                    .typography.bodySmall,
                color = MaterialTheme
                    .colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme
                    .outlineVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://uzair.is-a.dev"
                                )
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Portfolio",
                        style = MaterialTheme
                            .typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://linkedin.com/in/uzairmughal001"
                                )
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LinkedIn",
                        style = MaterialTheme
                            .typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse(
                                "mailto:contact@uzair.is-a.dev"
                            )
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "contact@uzair.is-a.dev",
                    style = MaterialTheme
                        .typography.labelMedium
                )
            }
        }
    }
}
