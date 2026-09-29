package com.aiedustudio.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

data class EditClip(val id: Long, val uri: String, val label: String, val kind: String, val durationMs: Long = 5000, val trimStartMs: Long = 0, val trimEndMs: Long = 5000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioApp() }
    }
}

@Composable
private fun StudioApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("studio_project", 0) }
    var clips by remember { mutableStateOf(emptyList<EditClip>()) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf("Ready • Local project") }
    var aspect by remember { mutableStateOf("16:9") }
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val newClips = uris.map { EditClip(System.nanoTime() + it.hashCode(), it.toString(), it.lastPathSegment ?: "Video", "VIDEO") }
        clips = clips + newClips
        if (selected == null) selected = newClips.firstOrNull()?.id
        if (newClips.isNotEmpty()) { player.setMediaItem(MediaItem.fromUri(Uri.parse(newClips.first().uri))); player.prepare() }
        status = "${newClips.size} media item(s) imported"
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        clips = clips + uris.map { EditClip(System.nanoTime() + it.hashCode(), it.toString(), it.lastPathSegment ?: "Image", "IMAGE", 5000, 0, 5000) }
        status = "Images added to timeline"
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        clips = clips + uris.map { EditClip(System.nanoTime() + it.hashCode(), it.toString(), it.lastPathSegment ?: "Audio", "AUDIO") }
        status = "Audio tracks added"
    }
    val active = clips.firstOrNull { it.id == selected }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF62D9B5), secondary = Color(0xFF8AB4F8),
        background = Color(0xFF101216), surface = Color(0xFF1A1E24)
    )) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101216)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("AI EDU STUDIO", color = Color(0xFF62D9B5), fontWeight = FontWeight.Bold)
                    Text("Untitled project", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = {
                    val arr = JSONArray()
                    clips.forEach { c -> arr.put(JSONObject().put("id",c.id).put("uri",c.uri).put("label",c.label).put("kind",c.kind).put("duration",c.durationMs).put("start",c.trimStartMs).put("end",c.trimEndMs)) }
                    prefs.edit().putString("clips",arr.toString()).putString("aspect",aspect).apply()
                    status = "Project saved on this device"
                }) { Text("SAVE") }
                TextButton(onClick = {
                    val arr = JSONArray(prefs.getString("clips","[]"))
                    clips = (0 until arr.length()).map { i -> val o=arr.getJSONObject(i); EditClip(o.getLong("id"),o.getString("uri"),o.getString("label"),o.getString("kind"),o.getLong("duration"),o.getLong("start"),o.getLong("end")) }
                    aspect = prefs.getString("aspect","16:9") ?: "16:9"
                    status = "Project loaded"
                }) { Text("OPEN") }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().weight(0.47f).background(Color.Black, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                if (active?.kind == "VIDEO") AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false } }, modifier = Modifier.fillMaxSize())
                else if (active?.kind == "IMAGE") AsyncImage(model = Uri.parse(active.uri), contentDescription = active.label, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = Color(0xFF62D9B5), modifier = Modifier.size(44.dp))
                    Text(active?.label ?: "Import media to begin", color = Color.White)
                    Text("Preview • ${aspect}", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.seekTo((player.currentPosition-5000).coerceAtLeast(0)) }) { Icon(Icons.Default.FastRewind, null, tint=Color.White) }
                IconButton(onClick = { if(player.isPlaying) player.pause() else player.play() }) { Icon(if(player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint=Color.White) }
                IconButton(onClick = { player.seekTo(player.currentPosition+5000) }) { Icon(Icons.Default.FastForward, null, tint=Color.White) }
                Spacer(Modifier.width(10.dp))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick={expanded=true}) { Text(aspect) }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                        listOf("16:9","9:16","1:1","4:5").forEach { a -> DropdownMenuItem(text={Text(a)},onClick={aspect=a;expanded=false}) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={videoPicker.launch(arrayOf("video/*"))}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.VideoLibrary,null); Spacer(Modifier.width(5.dp)); Text("Video") }
                OutlinedButton(onClick={imagePicker.launch(arrayOf("image/*"))}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.Image,null); Spacer(Modifier.width(5.dp)); Text("Images") }
                OutlinedButton(onClick={audioPicker.launch(arrayOf("audio/*"))}, modifier=Modifier.weight(1f)) { Icon(Icons.Default.AudioFile,null); Spacer(Modifier.width(5.dp)); Text("Audio") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                Text("TIMELINE", color=Color.White, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                Text("${clips.size} clips", color=Color.Gray, style=MaterialTheme.typography.labelSmall)
            }
            LazyColumn(Modifier.weight(0.3f).fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                items(clips, key={it.id}) { clip ->
                    Row(Modifier.fillMaxWidth().background(if(selected==clip.id) Color(0xFF25463F) else Color(0xFF20252D), RoundedCornerShape(8.dp)).clickable {
                        selected=clip.id
                        if(clip.kind=="VIDEO") { player.setMediaItem(MediaItem.fromUri(Uri.parse(clip.uri))); player.prepare() }
                    }.padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
                        Icon(when(clip.kind) { "AUDIO"->Icons.Default.GraphicEq; "IMAGE"->Icons.Default.Image; else->Icons.Default.Movie },null,tint=Color(0xFF62D9B5))
                        Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                            Text(clip.label, color=Color.White, maxLines=1)
                            Text("${clip.kind} • ${clip.durationMs/1000.0}s", color=Color.Gray, style=MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick={clips=clips.map { if(it.id==clip.id) it.copy(durationMs=(it.durationMs-1000).coerceAtLeast(1000),trimEndMs=(it.trimEndMs-1000).coerceAtLeast(1000)) else it };status="Duration adjusted"}) { Text("−1s") }
                        TextButton(onClick={clips=clips.map { if(it.id==clip.id) it.copy(durationMs=it.durationMs+1000,trimEndMs=it.trimEndMs+1000) else it };status="Duration adjusted"}) { Text("+1s") }
                        IconButton(onClick={
                            val i=clips.indexOfFirst { it.id==clip.id }
                            if(i>0) clips=clips.toMutableList().apply { add(i-1, removeAt(i)) }
                            status="Clip moved earlier"
                        }, enabled=clips.indexOfFirst { it.id==clip.id }>0) { Icon(Icons.Default.KeyboardArrowUp,null,tint=Color.White) }
                        IconButton(onClick={
                            val i=clips.indexOfFirst { it.id==clip.id }
                            if(i>=0 && i<clips.lastIndex) clips=clips.toMutableList().apply { add(i+1, removeAt(i)) }
                            status="Clip moved later"
                        }, enabled=clips.indexOfFirst { it.id==clip.id } in 0 until clips.lastIndex) { Icon(Icons.Default.KeyboardArrowDown,null,tint=Color.White) }
                        IconButton(onClick={clips=clips.filterNot { it.id==clip.id };if(selected==clip.id) selected=null}) { Icon(Icons.Default.Delete,null,tint=Color(0xFFFF7777)) }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Split","Duplicate","Trim in","Trim out","Text","Quiz","AI Tools","Export").forEach { tool ->
                    AssistChip(onClick={
                        val c=clips.firstOrNull{it.id==selected}
                        when(tool) {
                            "Split" -> if(c!=null && c.durationMs>=2000) { val half=c.durationMs/2; clips=clips.flatMap { if(it.id==c.id) listOf(c.copy(durationMs=half,trimEndMs=half),c.copy(id=System.nanoTime(),durationMs=c.durationMs-half,trimStartMs=half)) else listOf(it) }; status="Clip split at midpoint (timeline foundation)" }
                            "Duplicate" -> if(c!=null) { clips=clips + c.copy(id=System.nanoTime(),label=c.label+" copy"); status="Clip duplicated" }
                            "Trim in" -> if(c!=null) { clips=clips.map { if(it.id==c.id) it.copy(trimStartMs=(it.trimStartMs+500).coerceAtMost(it.trimEndMs-500)) else it }; status="Trim-in moved by 0.5s" }
                            "Trim out" -> if(c!=null) { clips=clips.map { if(it.id==c.id) it.copy(trimEndMs=(it.trimEndMs-500).coerceAtLeast(it.trimStartMs+500)) else it }; status="Trim-out moved by 0.5s" }
                            "Text" -> status="Text overlay editing is the next module"
                            "Quiz" -> {
                                val imgs = clips.filter { it.kind == "IMAGE" }
                                if (imgs.isNotEmpty()) {
                                    clips = clips.map { if (it.kind == "IMAGE") it.copy(durationMs = 5000, trimStartMs = 0, trimEndMs = 5000) else it }
                                    status = "Quiz sequence prepared: ${imgs.size} image cards • 5s each. Cards remain editable."
                                } else status = "Import images first to prepare a quiz sequence"
                            }
                            "AI Tools" -> status="AI services are not connected yet; manual edit remains available"
                            "Export" -> status="Media3 Transformer dependency included; render/export pipeline is not yet wired"
                        }
                    }, label = { Text(tool) })
                }
            }
            Text(status, color=Color(0xFF9AA4B2), style=MaterialTheme.typography.labelSmall, modifier=Modifier.padding(top=6.dp))
        }
    }
}
