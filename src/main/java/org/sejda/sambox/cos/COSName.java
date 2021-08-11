/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.cos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A PDF Name object.
 *
 * @author Ben Litchfield
 */
public final class COSName extends COSBase implements Comparable<COSName>
{
    private static Map<String, COSName> CUSTOM_NAMES = new ConcurrentHashMap<>(8192);
    private static Map<String, COSName> COMMON_NAMES = new HashMap<>();

    // A
    public static final COSName A = newCommonInstance("A");
    public static final COSName AA = newCommonInstance("AA");
    public static final COSName ABSOLUTE_COLORIMETRIC = newCommonInstance("AbsoluteColorimetric");
    public static final COSName AC = newCommonInstance("AC");
    public static final COSName ACRO_FORM = newCommonInstance("AcroForm");
    public static final COSName ACTUAL_TEXT = newCommonInstance("ActualText");
    public static final COSName ADBE_PKCS7_DETACHED = newCommonInstance("adbe.pkcs7.detached");
    public static final COSName ADBE_PKCS7_SHA1 = newCommonInstance("adbe.pkcs7.sha1");
    public static final COSName ADBE_X509_RSA_SHA1 = newCommonInstance("adbe.x509.rsa_sha1");
    public static final COSName ADOBE_PPKLITE = newCommonInstance("Adobe.PPKLite");
    public static final COSName AESV2 = newCommonInstance("AESV2");
    public static final COSName AESV3 = newCommonInstance("AESV3");
    public static final COSName AF = newCommonInstance("AF");
    public static final COSName AFTER = newCommonInstance("After");
    public static final COSName AIS = newCommonInstance("AIS");
    public static final COSName ALL_OFF = newCommonInstance("AllOff");
    public static final COSName ALL_ON = newCommonInstance("AllOn");
    public static final COSName ALT = newCommonInstance("Alt");
    public static final COSName ALPHA = newCommonInstance("Alpha");
    public static final COSName ALTERNATE = newCommonInstance("Alternate");
    public static final COSName ANNOT = newCommonInstance("Annot");
    public static final COSName ANNOTS = newCommonInstance("Annots");
    public static final COSName ANTI_ALIAS = newCommonInstance("AntiAlias");
    public static final COSName ANY_OFF = newCommonInstance("AnyOff");
    public static final COSName ANY_ON = newCommonInstance("AnyOn");
    public static final COSName AP = newCommonInstance("AP");
    public static final COSName AP_REF = newCommonInstance("APRef");
    public static final COSName APP = newCommonInstance("App");
    public static final COSName ART_BOX = newCommonInstance("ArtBox");
    public static final COSName ARTIFACT = newCommonInstance("Artifact");
    public static final COSName AS = newCommonInstance("AS");
    public static final COSName ASCENT = newCommonInstance("Ascent");
    public static final COSName ASCII_HEX_DECODE = newCommonInstance("ASCIIHexDecode");
    public static final COSName ASCII_HEX_DECODE_ABBREVIATION = newCommonInstance("AHx");
    public static final COSName ASCII85_DECODE = newCommonInstance("ASCII85Decode");
    public static final COSName ASCII85_DECODE_ABBREVIATION = newCommonInstance("A85");
    public static final COSName ATTACHED = newCommonInstance("Attached");
    public static final COSName AUTHOR = newCommonInstance("Author");
    public static final COSName AUTEVENT = newCommonInstance("AuthEvent");
    public static final COSName AVG_WIDTH = newCommonInstance("AvgWidth");
    // B
    public static final COSName B = newCommonInstance("B");
    public static final COSName BACKGROUND = newCommonInstance("Background");
    public static final COSName BASE_ENCODING = newCommonInstance("BaseEncoding");
    public static final COSName BASE_FONT = newCommonInstance("BaseFont");
    public static final COSName BASE_STATE = newCommonInstance("BaseState");
    public static final COSName BBOX = newCommonInstance("BBox");
    public static final COSName BC = newCommonInstance("BC");
    public static final COSName BE = newCommonInstance("BE");
    public static final COSName BEFORE = newCommonInstance("Before");
    public static final COSName BG = newCommonInstance("BG");
    public static final COSName BITS_PER_COMPONENT = newCommonInstance("BitsPerComponent");
    public static final COSName BITS_PER_COORDINATE = newCommonInstance("BitsPerCoordinate");
    public static final COSName BITS_PER_FLAG = newCommonInstance("BitsPerFlag");
    public static final COSName BITS_PER_SAMPLE = newCommonInstance("BitsPerSample");
    public static final COSName BLACK_IS_1 = newCommonInstance("BlackIs1");
    public static final COSName BLACK_POINT = newCommonInstance("BlackPoint");
    public static final COSName BLEED_BOX = newCommonInstance("BleedBox");
    public static final COSName BM = newCommonInstance("BM");
    public static final COSName BORDER = newCommonInstance("Border");
    public static final COSName BOUNDS = newCommonInstance("Bounds");
    public static final COSName BPC = newCommonInstance("BPC");
    public static final COSName BS = newCommonInstance("BS");
    public static final COSName BTN = newCommonInstance("Btn");
    public static final COSName BYTERANGE = newCommonInstance("ByteRange");
    // C
    public static final COSName C = newCommonInstance("C");
    public static final COSName C0 = newCommonInstance("C0");
    public static final COSName C1 = newCommonInstance("C1");
    public static final COSName CA = newCommonInstance("CA");
    public static final COSName CA_NS = newCommonInstance("ca");
    public static final COSName CALGRAY = newCommonInstance("CalGray");
    public static final COSName CALRGB = newCommonInstance("CalRGB");
    public static final COSName CAP = newCommonInstance("Cap");
    public static final COSName CAP_HEIGHT = newCommonInstance("CapHeight");
    public static final COSName CATALOG = newCommonInstance("Catalog");
    public static final COSName CCITTFAX_DECODE = newCommonInstance("CCITTFaxDecode");
    public static final COSName CCITTFAX_DECODE_ABBREVIATION = newCommonInstance("CCF");
    public static final COSName CENTER_WINDOW = newCommonInstance("CenterWindow");
    public static final COSName CF = newCommonInstance("CF");
    public static final COSName CFM = newCommonInstance("CFM");
    public static final COSName CH = newCommonInstance("Ch");
    public static final COSName CHAR_PROCS = newCommonInstance("CharProcs");
    public static final COSName CHAR_SET = newCommonInstance("CharSet");
    public static final COSName CI = newCommonInstance("CI");
    public static final COSName CICI_SIGNIT = newCommonInstance("CICI.SignIt");
    public static final COSName CID_FONT_TYPE0 = newCommonInstance("CIDFontType0");
    public static final COSName CID_FONT_TYPE2 = newCommonInstance("CIDFontType2");
    public static final COSName CID_TO_GID_MAP = newCommonInstance("CIDToGIDMap");
    public static final COSName CID_SET = newCommonInstance("CIDSet");
    public static final COSName CIDSYSTEMINFO = newCommonInstance("CIDSystemInfo");
    public static final COSName CL = newCommonInstance("CL");
    public static final COSName CLR_F = newCommonInstance("ClrF");
    public static final COSName CLR_FF = newCommonInstance("ClrFf");
    public static final COSName CMAP = newCommonInstance("CMap");
    public static final COSName CMAPNAME = newCommonInstance("CMapName");
    public static final COSName CMYK = newCommonInstance("CMYK");
    public static final COSName CO = newCommonInstance("CO");
    public static final COSName COLOR = newCommonInstance("Color");
    public static final COSName COLOR_BURN = newCommonInstance("ColorBurn");
    public static final COSName COLOR_DODGE = newCommonInstance("ColorDodge");
    public static final COSName COLORANTS = newCommonInstance("Colorants");
    public static final COSName COLORS = newCommonInstance("Colors");
    public static final COSName COLORSPACE = newCommonInstance("ColorSpace");
    public static final COSName COLUMNS = newCommonInstance("Columns");
    public static final COSName COMPATIBLE = newCommonInstance("Compatible");
    public static final COSName COMPONENTS = newCommonInstance("Components");
    public static final COSName CONTACT_INFO = newCommonInstance("ContactInfo");
    public static final COSName CONTENTS = newCommonInstance("Contents");
    public static final COSName COORDS = newCommonInstance("Coords");
    public static final COSName COUNT = newCommonInstance("Count");
    public static final COSName CP = newCommonInstance("CP");
    public static final COSName CREATION_DATE = newCommonInstance("CreationDate");
    public static final COSName CREATOR = newCommonInstance("Creator");
    public static final COSName CROP_BOX = newCommonInstance("CropBox");
    public static final COSName CRYPT = newCommonInstance("Crypt");
    public static final COSName CS = newCommonInstance("CS");
    // D
    public static final COSName D = newCommonInstance("D");
    public static final COSName DA = newCommonInstance("DA");
    public static final COSName DARKEN = newCommonInstance("Darken");
    public static final COSName DATAPREP = newCommonInstance("DataPrep");
    public static final COSName DATE = newCommonInstance("Date");
    public static final COSName DCT_DECODE = newCommonInstance("DCTDecode");
    public static final COSName DCT_DECODE_ABBREVIATION = newCommonInstance("DCT");
    public static final COSName DECODE = newCommonInstance("Decode");
    public static final COSName DECODE_PARMS = newCommonInstance("DecodeParms");
    public static final COSName DEFAULT = newCommonInstance("default");
    public static final COSName DEFAULT_CMYK = newCommonInstance("DefaultCMYK");
    public static final COSName DEFAULT_CRYPT_FILTER = newCommonInstance("DefaultCryptFilter");
    public static final COSName DEFAULT_GRAY = newCommonInstance("DefaultGray");
    public static final COSName DEFAULT_RGB = newCommonInstance("DefaultRGB");
    public static final COSName DESC = newCommonInstance("Desc");
    public static final COSName DESCENDANT_FONTS = newCommonInstance("DescendantFonts");
    public static final COSName DESCENT = newCommonInstance("Descent");
    public static final COSName DEST = newCommonInstance("Dest");
    public static final COSName DEST_OUTPUT_PROFILE = newCommonInstance("DestOutputProfile");
    public static final COSName DESTS = newCommonInstance("Dests");
    public static final COSName DEVICECMYK = newCommonInstance("DeviceCMYK");
    public static final COSName DEVICEGRAY = newCommonInstance("DeviceGray");
    public static final COSName DEVICEN = newCommonInstance("DeviceN");
    public static final COSName DEVICERGB = newCommonInstance("DeviceRGB");
    public static final COSName DI = newCommonInstance("Di");
    public static final COSName DIFFERENCE = newCommonInstance("Difference");
    public static final COSName DIFFERENCES = newCommonInstance("Differences");
    public static final COSName DIGEST_METHOD = newCommonInstance("DigestMethod");
    public static final COSName DIGEST_RIPEMD160 = newCommonInstance("RIPEMD160");
    public static final COSName DIGEST_SHA1 = newCommonInstance("SHA1");
    public static final COSName DIGEST_SHA256 = newCommonInstance("SHA256");
    public static final COSName DIGEST_SHA384 = newCommonInstance("SHA384");
    public static final COSName DIGEST_SHA512 = newCommonInstance("SHA512");
    public static final COSName DIRECTION = newCommonInstance("Direction");
    public static final COSName DISPLAY_DOC_TITLE = newCommonInstance("DisplayDocTitle");
    public static final COSName DL = newCommonInstance("DL");
    public static final COSName DM = newCommonInstance("Dm");
    public static final COSName DOC = newCommonInstance("Doc");
    public static final COSName DOC_CHECKSUM = newCommonInstance("DocChecksum");
    public static final COSName DOC_OPEN = newCommonInstance("DocOpen");
    public static final COSName DOC_TIME_STAMP = newCommonInstance("DocTimeStamp");
    public static final COSName DOCMDP = newCommonInstance("DocMDP");
    public static final COSName DOCUMENT = newCommonInstance("Document");
    public static final COSName DOMAIN = newCommonInstance("Domain");
    public static final COSName DOS = newCommonInstance("DOS");
    public static final COSName DP = newCommonInstance("DP");
    public static final COSName DR = newCommonInstance("DR");
    public static final COSName DS = newCommonInstance("DS");
    public static final COSName DUPLEX = newCommonInstance("Duplex");
    public static final COSName DUR = newCommonInstance("Dur");
    public static final COSName DV = newCommonInstance("DV");
    public static final COSName DW = newCommonInstance("DW");
    public static final COSName DW2 = newCommonInstance("DW2");
    // E
    public static final COSName E = newCommonInstance("E");
    public static final COSName EARLY_CHANGE = newCommonInstance("EarlyChange");
    public static final COSName EF = newCommonInstance("EF");
    public static final COSName EMBEDDED_FDFS = newCommonInstance("EmbeddedFDFs");
    public static final COSName EMBEDDED_FILE = newCommonInstance("EmbeddedFile");
    public static final COSName EMBEDDED_FILES = newCommonInstance("EmbeddedFiles");
    public static final COSName EMPTY = newCommonInstance("");
    public static final COSName ENCODE = newCommonInstance("Encode");
    public static final COSName ENCODED_BYTE_ALIGN = newCommonInstance("EncodedByteAlign");
    public static final COSName ENCODING = newCommonInstance("Encoding");
    public static final COSName ENCODING_90MS_RKSJ_H = newCommonInstance("90ms-RKSJ-H");
    public static final COSName ENCODING_90MS_RKSJ_V = newCommonInstance("90ms-RKSJ-V");
    public static final COSName ENCODING_ETEN_B5_H = newCommonInstance("ETen-B5-H");
    public static final COSName ENCODING_ETEN_B5_V = newCommonInstance("ETen-B5-V");
    public static final COSName ENCRYPT = newCommonInstance("Encrypt");
    public static final COSName ENCRYPT_META_DATA = newCommonInstance("EncryptMetadata");
    public static final COSName END_OF_LINE = newCommonInstance("EndOfLine");
    public static final COSName ENTRUST_PPKEF = newCommonInstance("Entrust.PPKEF");
    public static final COSName EXCLUSION = newCommonInstance("Exclusion");
    public static final COSName EXT_G_STATE = newCommonInstance("ExtGState");
    public static final COSName EXTEND = newCommonInstance("Extend");
    public static final COSName EXTENDS = newCommonInstance("Extends");
    // F
    public static final COSName F = newCommonInstance("F");
    public static final COSName F_DECODE_PARMS = newCommonInstance("FDecodeParms");
    public static final COSName F_FILTER = newCommonInstance("FFilter");
    public static final COSName FB = newCommonInstance("FB");
    public static final COSName FDF = newCommonInstance("FDF");
    public static final COSName FF = newCommonInstance("Ff");
    public static final COSName FIELDS = newCommonInstance("Fields");
    public static final COSName FILESPEC = newCommonInstance("Filespec");
    public static final COSName FILTER = newCommonInstance("Filter");
    public static final COSName FIRST = newCommonInstance("First");
    public static final COSName FIRST_CHAR = newCommonInstance("FirstChar");
    public static final COSName FIT_WINDOW = newCommonInstance("FitWindow");
    public static final COSName FL = newCommonInstance("FL");
    public static final COSName FLAGS = newCommonInstance("Flags");
    public static final COSName FLATE_DECODE = newCommonInstance("FlateDecode");
    public static final COSName FLATE_DECODE_ABBREVIATION = newCommonInstance("Fl");
    public static final COSName FONT = newCommonInstance("Font");
    public static final COSName FONT_BBOX = newCommonInstance("FontBBox");
    public static final COSName FONT_DESC = newCommonInstance("FontDescriptor");
    public static final COSName FONT_FAMILY = newCommonInstance("FontFamily");
    public static final COSName FONT_FILE = newCommonInstance("FontFile");
    public static final COSName FONT_FILE2 = newCommonInstance("FontFile2");
    public static final COSName FONT_FILE3 = newCommonInstance("FontFile3");
    public static final COSName FONT_MATRIX = newCommonInstance("FontMatrix");
    public static final COSName FONT_NAME = newCommonInstance("FontName");
    public static final COSName FONT_STRETCH = newCommonInstance("FontStretch");
    public static final COSName FONT_WEIGHT = newCommonInstance("FontWeight");
    public static final COSName FORM = newCommonInstance("Form");
    public static final COSName FORMTYPE = newCommonInstance("FormType");
    public static final COSName FRM = newCommonInstance("FRM");
    public static final COSName FT = newCommonInstance("FT");
    public static final COSName FUNCTION = newCommonInstance("Function");
    public static final COSName FUNCTION_TYPE = newCommonInstance("FunctionType");
    public static final COSName FUNCTIONS = newCommonInstance("Functions");
    // G
    public static final COSName G = newCommonInstance("G");
    public static final COSName GAMMA = newCommonInstance("Gamma");
    public static final COSName GROUP = newCommonInstance("Group");
    public static final COSName GTS_PDFA1 = newCommonInstance("GTS_PDFA1");
    // H
    public static final COSName H = newCommonInstance("H");
    public static final COSName HARD_LIGHT = newCommonInstance("HardLight");
    public static final COSName HEIGHT = newCommonInstance("Height");
    public static final COSName HELV = newCommonInstance("Helv");
    public static final COSName HIDE_MENUBAR = newCommonInstance("HideMenubar");
    public static final COSName HIDE_TOOLBAR = newCommonInstance("HideToolbar");
    public static final COSName HIDE_WINDOWUI = newCommonInstance("HideWindowUI");
    public static final COSName HUE = newCommonInstance("Hue");
    // I
    public static final COSName I = newCommonInstance("I");
    public static final COSName IC = newCommonInstance("IC");
    public static final COSName ICCBASED = newCommonInstance("ICCBased");
    public static final COSName ID = newCommonInstance("ID");
    public static final COSName ID_TREE = newCommonInstance("IDTree");
    public static final COSName IDENTITY = newCommonInstance("Identity");
    public static final COSName IDENTITY_H = newCommonInstance("Identity-H");
    public static final COSName IDENTITY_V = newCommonInstance("Identity-V");
    public static final COSName IF = newCommonInstance("IF");
    public static final COSName IM = newCommonInstance("IM");
    public static final COSName IMAGE = newCommonInstance("Image");
    public static final COSName IMAGE_MASK = newCommonInstance("ImageMask");
    public static final COSName INDEX = newCommonInstance("Index");
    public static final COSName INDEXED = newCommonInstance("Indexed");
    public static final COSName INFO = newCommonInstance("Info");
    public static final COSName INKLIST = newCommonInstance("InkList");
    public static final COSName INTENT = newCommonInstance("Intent");
    public static final COSName INTERPOLATE = newCommonInstance("Interpolate");
    public static final COSName IT = newCommonInstance("IT");
    public static final COSName ITALIC_ANGLE = newCommonInstance("ItalicAngle");
    public static final COSName ISSUER = newCommonInstance("Issuer");
    public static final COSName IX = newCommonInstance("IX");

    // J
    public static final COSName JAVA_SCRIPT = newCommonInstance("JavaScript");
    public static final COSName JBIG2_DECODE = newCommonInstance("JBIG2Decode");
    public static final COSName JBIG2_GLOBALS = newCommonInstance("JBIG2Globals");
    public static final COSName JPX_DECODE = newCommonInstance("JPXDecode");
    public static final COSName JS = newCommonInstance("JS");
    // K
    public static final COSName K = newCommonInstance("K");
    public static final COSName KEYWORDS = newCommonInstance("Keywords");
    public static final COSName KIDS = newCommonInstance("Kids");
    // L
    public static final COSName L = newCommonInstance("L");
    public static final COSName LAB = newCommonInstance("Lab");
    public static final COSName LANG = newCommonInstance("Lang");
    public static final COSName LAST = newCommonInstance("Last");
    public static final COSName LAST_CHAR = newCommonInstance("LastChar");
    public static final COSName LAST_MODIFIED = newCommonInstance("LastModified");
    public static final COSName LC = newCommonInstance("LC");
    public static final COSName LE = newCommonInstance("LE");
    public static final COSName LEADING = newCommonInstance("Leading");
    public static final COSName LEGAL_ATTESTATION = newCommonInstance("LegalAttestation");
    public static final COSName LENGTH = newCommonInstance("Length");
    public static final COSName LENGTH1 = newCommonInstance("Length1");
    public static final COSName LENGTH2 = newCommonInstance("Length2");
    public static final COSName LIGHTEN = newCommonInstance("Lighten");
    public static final COSName LIMITS = newCommonInstance("Limits");
    public static final COSName LJ = newCommonInstance("LJ");
    public static final COSName LL = newCommonInstance("LL");
    public static final COSName LLE = newCommonInstance("LLE");
    public static final COSName LLO = newCommonInstance("LLO");
    public static final COSName LOCATION = newCommonInstance("Location");
    public static final COSName LOCK = newCommonInstance("Lock");
    public static final COSName LUMINOSITY = newCommonInstance("Luminosity");
    public static final COSName LW = newCommonInstance("LW");
    public static final COSName LZW_DECODE = newCommonInstance("LZWDecode");
    public static final COSName LZW_DECODE_ABBREVIATION = newCommonInstance("LZW");
    // M
    public static final COSName M = newCommonInstance("M");
    public static final COSName MAC = newCommonInstance("Mac");
    public static final COSName MAC_EXPERT_ENCODING = newCommonInstance("MacExpertEncoding");
    public static final COSName MAC_ROMAN_ENCODING = newCommonInstance("MacRomanEncoding");
    public static final COSName MARK_INFO = newCommonInstance("MarkInfo");
    public static final COSName MASK = newCommonInstance("Mask");
    public static final COSName MATRIX = newCommonInstance("Matrix");
    public static final COSName MATTE = newCommonInstance("Matte");
    public static final COSName MAX_LEN = newCommonInstance("MaxLen");
    public static final COSName MAX_WIDTH = newCommonInstance("MaxWidth");
    public static final COSName MCID = newCommonInstance("MCID");
    public static final COSName MDP = newCommonInstance("MDP");
    public static final COSName MEDIA_BOX = newCommonInstance("MediaBox");
    public static final COSName MEASURE = newCommonInstance("Measure");
    public static final COSName METADATA = newCommonInstance("Metadata");
    public static final COSName MISSING_WIDTH = newCommonInstance("MissingWidth");
    public static final COSName MK = newCommonInstance("MK");
    public static final COSName ML = newCommonInstance("ML");
    public static final COSName MM_TYPE1 = newCommonInstance("MMType1");
    public static final COSName MOD_DATE = newCommonInstance("ModDate");
    public static final COSName MULTIPLY = newCommonInstance("Multiply");
    // N
    public static final COSName N = newCommonInstance("N");
    public static final COSName NAME = newCommonInstance("Name");
    public static final COSName NAMES = newCommonInstance("Names");
    public static final COSName NEED_APPEARANCES = newCommonInstance("NeedAppearances");
    public static final COSName NEW_WINDOW = newCommonInstance("NewWindow");
    public static final COSName NEXT = newCommonInstance("Next");
    public static final COSName NM = newCommonInstance("NM");
    public static final COSName NON_EFONT_NO_WARN = newCommonInstance("NonEFontNoWarn");
    public static final COSName NON_FULL_SCREEN_PAGE_MODE = newCommonInstance(
            "NonFullScreenPageMode");
    public static final COSName NONE = newCommonInstance("None");
    public static final COSName NORMAL = newCommonInstance("Normal");
    public static final COSName NUMS = newCommonInstance("Nums");
    // O
    public static final COSName O = newCommonInstance("O");
    public static final COSName OBJ = newCommonInstance("Obj");
    public static final COSName OBJ_STM = newCommonInstance("ObjStm");
    public static final COSName OC = newCommonInstance("OC");
    public static final COSName OCG = newCommonInstance("OCG");
    public static final COSName OCGS = newCommonInstance("OCGs");
    public static final COSName OCMD = newCommonInstance("OCMD");
    public static final COSName OCPROPERTIES = newCommonInstance("OCProperties");
    public static final COSName OE = newCommonInstance("OE");
    public static final COSName OFF = newCommonInstance("OFF");
    public static final COSName Off = newCommonInstance("Off");
    public static final COSName ON = newCommonInstance("ON");
    public static final COSName OP = newCommonInstance("OP");
    public static final COSName OP_NS = newCommonInstance("op");
    public static final COSName OPEN_ACTION = newCommonInstance("OpenAction");
    public static final COSName OPEN_TYPE = newCommonInstance("OpenType");
    public static final COSName OPM = newCommonInstance("OPM");
    public static final COSName OPT = newCommonInstance("Opt");
    public static final COSName ORDER = newCommonInstance("Order");
    public static final COSName ORDERING = newCommonInstance("Ordering");
    public static final COSName OS = newCommonInstance("OS");
    public static final COSName OUTLINES = newCommonInstance("Outlines");
    public static final COSName OUTPUT_CONDITION = newCommonInstance("OutputCondition");
    public static final COSName OUTPUT_CONDITION_IDENTIFIER = newCommonInstance(
            "OutputConditionIdentifier");
    public static final COSName OUTPUT_INTENT = newCommonInstance("OutputIntent");
    public static final COSName OUTPUT_INTENTS = newCommonInstance("OutputIntents");
    public static final COSName OVERLAY = newCommonInstance("Overlay");
    // P
    public static final COSName P = newCommonInstance("P");
    public static final COSName PAGE = newCommonInstance("Page");
    public static final COSName PAGE_LABELS = newCommonInstance("PageLabels");
    public static final COSName PAGE_LAYOUT = newCommonInstance("PageLayout");
    public static final COSName PAGE_MODE = newCommonInstance("PageMode");
    public static final COSName PAGES = newCommonInstance("Pages");
    public static final COSName PAINT_TYPE = newCommonInstance("PaintType");
    public static final COSName PANOSE = newCommonInstance("Panose");
    public static final COSName PARAMS = newCommonInstance("Params");
    public static final COSName PARENT = newCommonInstance("Parent");
    public static final COSName PARENT_TREE = newCommonInstance("ParentTree");
    public static final COSName PARENT_TREE_NEXT_KEY = newCommonInstance("ParentTreeNextKey");
    public static final COSName PATH = newCommonInstance("Path");
    public static final COSName PATTERN = newCommonInstance("Pattern");
    public static final COSName PATTERN_TYPE = newCommonInstance("PatternType");
    public static final COSName PDF_DOC_ENCODING = newCommonInstance("PDFDocEncoding");
    public static final COSName PERMS = newCommonInstance("Perms");
    public static final COSName PERCEPTUAL = newCommonInstance("Perceptual");
    public static final COSName PG = newCommonInstance("Pg");
    public static final COSName PIECE_INFO = newCommonInstance("PieceInfo");
    public static final COSName PMD = newCommonInstance("PMD");
    public static final COSName POPUP = newCommonInstance("Popup");
    public static final COSName PRE_RELEASE = newCommonInstance("PreRelease");
    public static final COSName PREDICTOR = newCommonInstance("Predictor");
    public static final COSName PREV = newCommonInstance("Prev");
    public static final COSName PRINT_AREA = newCommonInstance("PrintArea");
    public static final COSName PRINT_CLIP = newCommonInstance("PrintClip");
    public static final COSName PRINT_SCALING = newCommonInstance("PrintScaling");
    public static final COSName PROC_SET = newCommonInstance("ProcSet");
    public static final COSName PROCESS = newCommonInstance("Process");
    public static final COSName PRODUCER = newCommonInstance("Producer");
    public static final COSName PROP_BUILD = newCommonInstance("Prop_Build");
    public static final COSName PROPERTIES = newCommonInstance("Properties");
    public static final COSName PS = newCommonInstance("PS");
    public static final COSName PUB_SEC = newCommonInstance("PubSec");
    // Q
    public static final COSName Q = newCommonInstance("Q");
    public static final COSName QUADPOINTS = newCommonInstance("QuadPoints");
    // R
    public static final COSName R = newCommonInstance("R");
    public static final COSName RANGE = newCommonInstance("Range");
    public static final COSName RC = newCommonInstance("RC");
    public static final COSName RD = newCommonInstance("RD");
    public static final COSName REASON = newCommonInstance("Reason");
    public static final COSName REASONS = newCommonInstance("Reasons");
    public static final COSName RELATIVE_COLORIMETRIC = newCommonInstance("RelativeColorimetric");
    public static final COSName RECIPIENTS = newCommonInstance("Recipients");
    public static final COSName RECT = newCommonInstance("Rect");
    public static final COSName REGISTRY = newCommonInstance("Registry");
    public static final COSName REGISTRY_NAME = newCommonInstance("RegistryName");
    public static final COSName RENAME = newCommonInstance("Rename");
    public static final COSName RESOURCES = newCommonInstance("Resources");
    public static final COSName RGB = newCommonInstance("RGB");
    public static final COSName RI = newCommonInstance("RI");
    public static final COSName ROLE_MAP = newCommonInstance("RoleMap");
    public static final COSName ROOT = newCommonInstance("Root");
    public static final COSName ROTATE = newCommonInstance("Rotate");
    public static final COSName ROWS = newCommonInstance("Rows");
    public static final COSName RUN_LENGTH_DECODE = newCommonInstance("RunLengthDecode");
    public static final COSName RUN_LENGTH_DECODE_ABBREVIATION = newCommonInstance("RL");
    public static final COSName RV = newCommonInstance("RV");
    // S
    public static final COSName S = newCommonInstance("S");
    public static final COSName SA = newCommonInstance("SA");
    public static final COSName SATURATION = newCommonInstance("Saturation");
    public static final COSName SCREEN = newCommonInstance("Screen");
    public static final COSName SE = newCommonInstance("SE");
    public static final COSName SEPARATION = newCommonInstance("Separation");
    public static final COSName SET_F = newCommonInstance("SetF");
    public static final COSName SET_FF = newCommonInstance("SetFf");
    public static final COSName SHADING = newCommonInstance("Shading");
    public static final COSName SHADING_TYPE = newCommonInstance("ShadingType");
    public static final COSName SIG = newCommonInstance("Sig");
    public static final COSName SIG_FLAGS = newCommonInstance("SigFlags");
    public static final COSName SIZE = newCommonInstance("Size");
    public static final COSName SM = newCommonInstance("SM");
    public static final COSName SMASK = newCommonInstance("SMask");
    public static final COSName SOFT_LIGHT = newCommonInstance("SoftLight");
    public static final COSName SOUND = newCommonInstance("Sound");
    public static final COSName SS = newCommonInstance("SS");
    public static final COSName ST = newCommonInstance("St");
    public static final COSName STANDARD = newCommonInstance("Standard");
    public static final COSName STANDARD_ENCODING = newCommonInstance("StandardEncoding");
    public static final COSName STATE = newCommonInstance("State");
    public static final COSName STATE_MODEL = newCommonInstance("StateModel");
    public static final COSName STATUS = newCommonInstance("Status");
    public static final COSName STD_CF = newCommonInstance("StdCF");
    public static final COSName STEM_H = newCommonInstance("StemH");
    public static final COSName STEM_V = newCommonInstance("StemV");
    public static final COSName STM_F = newCommonInstance("StmF");
    public static final COSName STR_F = newCommonInstance("StrF");
    public static final COSName STRUCT_PARENT = newCommonInstance("StructParent");
    public static final COSName STRUCT_PARENTS = newCommonInstance("StructParents");
    public static final COSName STRUCT_TREE_ROOT = newCommonInstance("StructTreeRoot");
    public static final COSName STYLE = newCommonInstance("Style");
    public static final COSName SUB_FILTER = newCommonInstance("SubFilter");
    public static final COSName SUBJ = newCommonInstance("Subj");
    public static final COSName SUBJECT = newCommonInstance("Subject");
    public static final COSName SUBTYPE = newCommonInstance("Subtype");
    public static final COSName SUPPLEMENT = newCommonInstance("Supplement");
    public static final COSName SV = newCommonInstance("SV");
    public static final COSName SW = newCommonInstance("SW");
    // T
    public static final COSName T = newCommonInstance("T");
    public static final COSName TARGET = newCommonInstance("Target");
    public static final COSName TEMPLATES = newCommonInstance("Templates");
    public static final COSName THREADS = newCommonInstance("Threads");
    public static final COSName TI = newCommonInstance("TI");
    public static final COSName TILING_TYPE = newCommonInstance("TilingType");
    public static final COSName TIME_STAMP = newCommonInstance("TimeStamp");
    public static final COSName TITLE = newCommonInstance("Title");
    public static final COSName TK = newCommonInstance("TK");
    public static final COSName TM = newCommonInstance("TM");
    public static final COSName TO_UNICODE = newCommonInstance("ToUnicode");
    public static final COSName TR = newCommonInstance("TR");
    public static final COSName TR2 = newCommonInstance("TR2");
    public static final COSName TRAPPED = newCommonInstance("Trapped");
    public static final COSName TRANS = newCommonInstance("Trans");
    public static final COSName TRANSPARENCY = newCommonInstance("Transparency");
    public static final COSName TREF = newCommonInstance("TRef");
    public static final COSName TRIM_BOX = newCommonInstance("TrimBox");
    public static final COSName TRUE_TYPE = newCommonInstance("TrueType");
    public static final COSName TRUSTED_MODE = newCommonInstance("TrustedMode");
    public static final COSName TU = newCommonInstance("TU");
    /** Acro form field type for text field. */
    public static final COSName TX = newCommonInstance("Tx");
    public static final COSName TYPE = newCommonInstance("Type");
    public static final COSName TYPE0 = newCommonInstance("Type0");
    public static final COSName TYPE1 = newCommonInstance("Type1");
    public static final COSName TYPE3 = newCommonInstance("Type3");
    // U
    public static final COSName U = newCommonInstance("U");
    public static final COSName UE = newCommonInstance("UE");
    public static final COSName UF = newCommonInstance("UF");
    public static final COSName UNCHANGED = newCommonInstance("Unchanged");
    public static final COSName UNIX = newCommonInstance("Unix");
    public static final COSName URI = newCommonInstance("URI");
    public static final COSName URL = newCommonInstance("URL");
    public static final COSName URL_TYPE = newCommonInstance("URLType");
    public static final COSName USER_UNIT = newCommonInstance("UserUnit");
    // V
    public static final COSName V = newCommonInstance("V");
    public static final COSName V2 = newCommonInstance("V2");
    public static final COSName VE = newCommonInstance("VE");
    public static final COSName VERISIGN_PPKVS = newCommonInstance("VeriSign.PPKVS");
    public static final COSName VERSION = newCommonInstance("Version");
    public static final COSName VERTICES = newCommonInstance("Vertices");
    public static final COSName VERTICES_PER_ROW = newCommonInstance("VerticesPerRow");
    public static final COSName VIEW_AREA = newCommonInstance("ViewArea");
    public static final COSName VIEW_CLIP = newCommonInstance("ViewClip");
    public static final COSName VIEWER_PREFERENCES = newCommonInstance("ViewerPreferences");
    public static final COSName VOLUME = newCommonInstance("Volume");
    public static final COSName VP = newCommonInstance("VP");
    // W
    public static final COSName W = newCommonInstance("W");
    public static final COSName W2 = newCommonInstance("W2");
    public static final COSName WHITE_POINT = newCommonInstance("WhitePoint");
    public static final COSName WIDGET = newCommonInstance("Widget");
    public static final COSName WIDTH = newCommonInstance("Width");
    public static final COSName WIDTHS = newCommonInstance("Widths");
    public static final COSName WIN_ANSI_ENCODING = newCommonInstance("WinAnsiEncoding");
    // X
    public static final COSName XFA = newCommonInstance("XFA");
    public static final COSName X_STEP = newCommonInstance("XStep");
    public static final COSName XHEIGHT = newCommonInstance("XHeight");
    public static final COSName XOBJECT = newCommonInstance("XObject");
    public static final COSName XREF = newCommonInstance("XRef");
    public static final COSName XREF_STM = newCommonInstance("XRefStm");
    // Y
    public static final COSName Y_STEP = newCommonInstance("YStep");
    public static final COSName YES = newCommonInstance("Yes");

    public static final COSName ZA_DB = newCommonInstance("ZaDb");

    private final String name;

    /**
     * This will get a COSName object with that name.
     * 
     * @param aName The name of the object.
     * 
     * @return A COSName with the specified name.
     */
    public static COSName getPDFName(String aName)
    {
        if (aName != null)
        {
            COSName cosName = COMMON_NAMES.get(aName);
            if (cosName != null)
            {
                return cosName;
            }
            return getCustom(aName);
        }
        return null;
    }

    private static COSName getCustom(String customName)
    {
        COSName cosName = CUSTOM_NAMES.get(customName);
        if (cosName == null)
        {
            final COSName value = new COSName(customName);
            cosName = CUSTOM_NAMES.putIfAbsent(customName, value);
            if (cosName == null)
            {
                cosName = value;
            }
        }
        return cosName;
    }

    private static COSName newCommonInstance(String commonName)
    {
        final COSName value = new COSName(commonName);
        COMMON_NAMES.put(commonName, value);
        return value;
    }

    private COSName(String name)
    {
        this.name = name;
    }

    /**
     * @return The name of the object.
     */
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "COSName{" + name + "}";
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof COSName && name.equals(((COSName) object).name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public int compareTo(COSName other)
    {
        return name.compareTo(other.name);
    }

    @Override
    public void accept(COSVisitor visitor) throws IOException
    {
        visitor.visit(this);
    }

    @Override
    public void idIfAbsent(IndirectCOSObjectIdentifier id)
    {
        // we don't store id for names. We write them as direct objects. Wrap this with an IndirectCOSObject if you
        // want to write a number as indirect reference
    }
}
