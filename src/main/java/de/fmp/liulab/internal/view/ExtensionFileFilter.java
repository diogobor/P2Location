package de.fmp.liulab.internal.view;
 import java.io.File;  
 import java.util.List;  
   
 /**
  * File filter
  */ 
 public class ExtensionFileFilter extends javax.swing.filechooser.FileFilter  
 {  
     String[] extensions;  
     String description;  
   
     /**
      * Creates the file filter with many different extensions 
      * @param descr description
      * @param exts one or more extensions
      */
     public ExtensionFileFilter(String descr, String... exts)  
     {  
         // clone and lowercase the extensions  
         extensions = new String[exts.length];  
         for (int i = exts.length - 1; i >= 0; i--)  
             extensions[i] = exts[i].toLowerCase();  
   
         // make sure we have a valid (if simplistic) description  
         description = (descr == null ? exts[0] + " files" : descr);  
     }  
   
     /** 
      * Creates a file filter with several extensions. 
      *  
      * @param descr The filter description. 
      * @param exts The extensions that the filter supports. 
      */  
     public ExtensionFileFilter(String descr, List<String> exts)  
     {  
         this(descr, exts.toArray(new String[exts.size()]));  
     }  
   
     /** 
      * Verify if its a valid file. This method is automatically called by the 
      * <code>FileChooser</code> object. 
      *  
      * @param f The file to be verified. 
      * @return A boolean indicated if the file was accepted by the filter or 
      *         not. 
      */  
     @Override  
     public boolean accept(File f)  
     {  
         // we always allow directories, regardless of their extension  
         if (f.isDirectory())  
             return true;  
   
         // ok, it's a regular file so check the extension  
         for (String extension : extensions)  
             if (f.getName().toLowerCase().endsWith(extension))  
                 return true;  
   
         return false;  
     }  
   
     /** 
      * The description of this kind of files. 
      *  
      * @return file kind description 
      */  
     @Override  
     public String getDescription()  
     {  
         return description;  
     }  
 } 