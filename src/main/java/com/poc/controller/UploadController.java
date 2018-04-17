package com.poc.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.icc.ICCProfile;
import com.icafe4j.image.options.JPEGOptions;
import com.icafe4j.image.options.PNGOptions;
import com.icafe4j.image.options.TIFFOptions;
import com.icafe4j.image.png.Filter;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.quant.QuantMethod;
import com.icafe4j.image.reader.ImageReader;
import com.icafe4j.image.tiff.TiffFieldEnum.Compression;
import com.icafe4j.image.tiff.TiffFieldEnum.PhotoMetric;
import com.icafe4j.io.ByteOrder;
import com.icafe4j.io.PeekHeadInputStream;

@Controller
public class UploadController {

	private static Logger logger = LoggerFactory.getLogger(UploadController.class);

	@GetMapping("/")
	public String index() {
		return "upload";
	}

	@PostMapping("/upload")
	public String singleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
			return "redirect:uploadStatus";
		}

		try {
			
			convert(file.getInputStream(), ImageType.JPG);
			convert(file.getInputStream(), ImageType.TIFF);
			convert(file.getInputStream(), ImageType.PNG);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded '" + file.getOriginalFilename() + "'");

		return "redirect:/uploadStatus";
	}

	@GetMapping("/uploadStatus")
	public String uploadStatus() {
		return "uploadStatus";
	}

	
	/**
	 * ImageConverter using iCafe
	 */
	private void convert(InputStream inputStream, ImageType imageType) throws Exception {
		 
		long t1 = System.currentTimeMillis();
		 
		 PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(inputStream, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		 ImageReader reader = ImageIO.getReader(peekHeadInputStream);
		 BufferedImage img = reader.read(peekHeadInputStream);		 
		 peekHeadInputStream.close();
	
		 if(img == null) {
			 logger.error("Failed reading image!");
			 return;
		 }
		 
		 logger.debug("Total frames read: {}", reader.getFrameCount());
		 logger.debug("Color model: {}", img.getColorModel());
		 logger.debug("Raster: {}", img.getRaster());
		 logger.debug("Sample model: {}", img.getSampleModel());
		
		 long t2 = System.currentTimeMillis();
		 
		 logger.debug("decoding time {}ms", (t2-t1));
		
		 FileOutputStream fo = new FileOutputStream("NEW_iCafe_" +Math.random() +"." + imageType.getExtension());
				
		 ImageParam.ImageParamBuilder builder = ImageParam.getBuilder();
		  
		 switch(imageType) {
		  	case TIFF: // Set TIFF-specific options
		  		 TIFFOptions tiffOptions = new TIFFOptions();
		  		 tiffOptions.setByteOrder(ByteOrder.LITTLE_ENDIAN);
		  		 tiffOptions.setApplyPredictor(true);
		  		 tiffOptions.setTiffCompression(Compression.LZW);
		  		 tiffOptions.setJPEGQuality(100);
		  		 tiffOptions.setPhotoMetric(PhotoMetric.SEPARATED);
		  		 tiffOptions.setWriteICCProfile(true);
		  		 tiffOptions.setDeflateCompressionLevel(6);
		  		 tiffOptions.setXResolution(96);
		  		 tiffOptions.setYResolution(96);
		  		 
		  		 builder.imageOptions(tiffOptions);
		  		 break;
		  	case PNG: // Set PNG-specific options
		  		PNGOptions pngOptions = new PNGOptions();
		  		pngOptions.setApplyAdaptiveFilter(false);
		  		pngOptions.setCompressionLevel(6);
		  		pngOptions.setFilterType(Filter.NONE);
		  		
		  		builder.imageOptions(pngOptions);
		  		break;
		  	case JPG: // Set JPG-specific options
		  		JPEGOptions jpegOptions = new JPEGOptions();
		  		jpegOptions.setQuality(100);
		  		jpegOptions.setColorSpace(JPEGOptions.COLOR_SPACE_YCbCr);
		  		jpegOptions.setWriteICCProfile(true);
		  		jpegOptions.setTiffFlavor(true);
		  		
		  		builder.imageOptions(jpegOptions);
		  		break;
		  	default:
		 }
		  
		 t1 = System.currentTimeMillis();
		 
		 ImageIO.write(img, fo, imageType, builder
								 .quantMethod(QuantMethod.WU_QUANT)
								 .colorType(ImageColorType.INDEXED)
								 .applyDither(true)
								 .ditherMethod(DitherMethod.FLOYD_STEINBERG)
								 .hasAlpha(true)
								 .build());		
		 
		 t2 = System.currentTimeMillis();
		
		 fo.close();
		
		 logger.debug("{} writer (encoding time {}ms)", imageType, (t2-t1));
		
	 }
	
	/**
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
	private void testICCProfile(InputStream inputStream) throws IOException {		
		
		Metadata icc_profile = new ICCProfile(inputStream);
		icc_profile.showMetadata();
		
		FileOutputStream fout = new FileOutputStream(new File("poc_ICCProfile_"+Math.random()+".icc"));
		icc_profile.write(fout);
	
		fout.close();
	}
	
	/**
	 * ImageConverter using TwelveMonkeys
	 */
	/*
	private void convert(InputStream inputStream) {

		try {
			
			BufferedImage image = ImageIO.read(inputStream);

			logger.info("Image - Type - {}", image.getType());

			logger.info("ColorSpace - Type - {}", image.getColorModel().getColorSpace().getType());
			logger.info("ColorSpace - isCS_sRGB - {}", image.getColorModel().getColorSpace().isCS_sRGB());
			logger.info("ColorSpace - hasAlpha - {}", image.getColorModel().hasAlpha());

			logger.info("Color Model - isAlphaPremultiplied - {}", image.getColorModel().isAlphaPremultiplied());

			logger.info("ColorSpace - 1 - {}", image.getColorModel().getColorSpace());
			logger.info("ColorSpace - 2 - {}", ColorModel.getRGBdefault().getColorSpace() );
			logger.info("ColorSpace - 3 - {}", image.getColorModel().getColorSpace().getType());

			if (!ImageIO.write(image, "png", new File("/test.png"))) {
				// Handle image not written case
				logger.info("conversion didn't work for png...");
			}
			
			if (!ImageIO.write(image, "tiff", new File("/test.tiff"))) {
				// Handle image not written case
				logger.info("conversion didn't work for tiff...");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
}