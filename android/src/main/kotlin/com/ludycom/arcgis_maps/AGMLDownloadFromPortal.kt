package com.ludycom.arcgis_maps

import android.content.Context
import com.arcgismaps.mapping.PortalItem
import com.ludycom.arcgis_maps.entities.AGMLPortalItem
import com.ludycom.arcgis_maps.entities.AGMLDownloadPortalItem
import com.ludycom.arcgis_maps.utils.AGMLDownloadStatusEnum
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream



class AGMLDownloadFromPortal (private val context: Context) {

    private fun getDownloaderFolder(): String {
        return context.getExternalFilesDir(null)?.absolutePath.toString()+File.separator+"Portal Items"
    }

    suspend fun downloadPortalItem(aGMLPortalItem: AGMLPortalItem): AGMLDownloadPortalItem {
        val folderPath = getDownloaderFolder()

        val provisionFolder = File(folderPath)
        if (!provisionFolder.exists()) {
            provisionFolder.mkdirs()
        }

        val aGMLDownloadPortalItem = AGMLDownloadPortalItem(
            portalItem = aGMLPortalItem,
            pathLocation = ""
        )

        val portalItem = PortalItem(aGMLPortalItem.url)
        portalItem.load().onSuccess {
            portalItem.fetchData().onSuccess { byteArrayData ->

                kotlin.runCatching {
                    val byteArrayInputStream = ByteArrayInputStream(byteArrayData)
                    val data = ByteArray(1024)
                    var downloadCount: Int
                    downloadCount = byteArrayInputStream.read(data)
                    while (downloadCount != -1) {
                        downloadCount = byteArrayInputStream.read(data)
                    }

                    val folderLayer = File(provisionFolder.path+File.separator+portalItem.itemId)
                    if (!folderLayer.exists()) {
                        folderLayer.mkdirs()
                    }

                    val destinationFilePath = folderLayer.path + File.separator + portalItem.name
                    val provisionFile = File(destinationFilePath)

                    provisionFile.createNewFile()

                    FileOutputStream(provisionFile).use { out ->
                        out.write(byteArrayData)

                        if (portalItem.name.contains(".zip")) {
                            val fileInputStream = FileInputStream(destinationFilePath)
                            val zipInputStream = ZipInputStream(BufferedInputStream(fileInputStream))
                            var zipEntry: ZipEntry? = zipInputStream.nextEntry
                            val buffer = ByteArray(1024)

                            while (zipEntry != null) {
                                if (zipEntry.isDirectory) {
                                    File(folderLayer.path, zipEntry.name).mkdirs()
                                } else {
                                    val file = File(folderLayer.path, zipEntry.name)
                                    val fileOut = FileOutputStream(file)
                                    var count = zipInputStream.read(buffer)
                                    while (count != -1) {
                                        fileOut.write(buffer, 0, count)
                                        count = zipInputStream.read(buffer)
                                    }
                                    fileOut.close()
                                    if(!aGMLDownloadPortalItem.pathLocation.contains(".shp")) {
                                        aGMLDownloadPortalItem.pathLocation = file.path;
                                    }
                                }

                                zipInputStream.closeEntry()
                                zipEntry = zipInputStream.nextEntry
                            }

                            zipInputStream.close()
                            FileUtils.delete(provisionFile)
                        } else {
                            aGMLDownloadPortalItem.pathLocation = destinationFilePath
                        }
                        aGMLDownloadPortalItem.downloadStatus = AGMLDownloadStatusEnum.SUCCESS

                        return aGMLDownloadPortalItem
                    }
                }
            }
        }

        aGMLDownloadPortalItem.downloadStatus = AGMLDownloadStatusEnum.FAILED
        return aGMLDownloadPortalItem
    }
}