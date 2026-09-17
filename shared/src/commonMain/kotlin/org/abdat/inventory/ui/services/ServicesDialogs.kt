package org.abdat.inventory.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.abdat.inventory.domain.model.Local
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.theme.AppTheme

@Composable
fun AddServiceDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, localNum: String) -> Unit
) {
    if (!isOpen) return
    var serviceName by remember { mutableStateOf("") }
    var localNum by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Nouveau Service Demandeur",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Nom du Service") },
                    placeholder = { Text("Ex: Service d'Oncologie Médicale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = localNum,
                    onValueChange = { localNum = it },
                    label = { Text("Code / N° Local Principal (Optionnel)") },
                    placeholder = { Text("Ex: SRV-ONCO-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (serviceName.isNotBlank()) {
                                onConfirm(serviceName, localNum)
                            }
                        },
                        enabled = serviceName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Créer le Service")
                    }
                }
            }
        }
    }
}

@Composable
fun EditServiceDialog(
    isOpen: Boolean,
    service: Service?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, localNum: String) -> Unit
) {
    if (!isOpen || service == null) return
    var serviceName by remember(service.id) { mutableStateOf(service.name) }
    var localNum by remember(service.id) { mutableStateOf(service.localNum) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifier le Service",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Nom du Service") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = localNum,
                    onValueChange = { localNum = it },
                    label = { Text("Code / N° Local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (serviceName.isNotBlank()) {
                                onConfirm(serviceName, localNum)
                            }
                        },
                        enabled = serviceName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteServiceDialog(
    isOpen: Boolean,
    service: Service?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isOpen || service == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(440.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Supprimer le Service ?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Êtes-vous sûr de vouloir supprimer le service « ${service.name} » ? Cette action supprimera également tous ses locaux rattachés. Les services déjà associés à des bons de sortie existants ne peuvent pas être supprimés.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer définitivement")
                    }
                }
            }
        }
    }
}

@Composable
fun AddLocalDialog(
    isOpen: Boolean,
    service: Service?,
    onDismiss: () -> Unit,
    onConfirm: (localName: String) -> Unit
) {
    if (!isOpen || service == null) return
    var localName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajouter un Local pour « ${service.name} »",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )

                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    label = { Text("Désignation du Local / Salle") },
                    placeholder = { Text("Ex: Pharmacie de Service - Placard A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (localName.isNotBlank()) {
                                onConfirm(localName)
                            }
                        },
                        enabled = localName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}

@Composable
fun EditLocalDialog(
    isOpen: Boolean,
    local: Local?,
    onDismiss: () -> Unit,
    onConfirm: (localName: String) -> Unit
) {
    if (!isOpen || local == null) return
    var localName by remember(local.id) { mutableStateOf(local.name) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modifier la Désignation du Local",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )

                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    label = { Text("Désignation du Local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (localName.isNotBlank()) {
                                onConfirm(localName)
                            }
                        },
                        enabled = localName.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteLocalDialog(
    isOpen: Boolean,
    local: Local?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isOpen || local == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Supprimer le Local ?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Êtes-vous sûr de vouloir supprimer le local « ${local.name} » ? Les bons de sortie déjà passés avec ce local conserveront leur historique.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Supprimer")
                    }
                }
            }
        }
    }
}
