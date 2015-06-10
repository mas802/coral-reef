/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coral.reef.web;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class FileUploadController {

    @RequestMapping(value = "/admin/upload", method = RequestMethod.GET)
    public String provideUploadInfo() {
        return "upload";
    }

    @RequestMapping(value = "/admin/upload", method = RequestMethod.POST)
    public String handleFileUpload(@RequestParam("name") String name,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            byte[] bytes = file.getBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            name = name.replaceAll("[^a-zA-Z0-9_]", "");
            if (name.length() > 10) {
                name = name.substring(0, 10);
            }

            File target = new File("uploads", name);
            target.mkdirs();
            unzip(bis, target);

            return "redirect:/admin/edit?name=" + name + "&basepath="
                    + target.getAbsolutePath();
        } else {
            throw new RuntimeException("uplodaed file contains no data");
        }
    }

    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified
     * by destDirectory (will be created if does not exists)
     * 
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public void unzip(InputStream zipFilePath, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(zipFilePath);
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            File filePath = new File(destDir, entry.getName());
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                filePath.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     * 
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private void extractFile(ZipInputStream zipIn, File filePath)
            throws IOException {
        filePath.getParentFile().mkdirs();
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

}
