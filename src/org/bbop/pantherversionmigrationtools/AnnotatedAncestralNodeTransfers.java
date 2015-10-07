/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.bbop.pantherversionmigrationtools;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;

import owltools.gaf.GafDocument;
import owltools.gaf.GeneAnnotation;
import owltools.gaf.io.GafWriter;
import owltools.gaf.io.ResourceLoader;
import owltools.gaf.parser.GafObjectsBuilder;

public class AnnotatedAncestralNodeTransfers {
	/**
	 * Method declaration
	 *
	 *
	 * @param args either a input file, or the triplet of PTN,old_tree,new_tree
	 *
	 * @see
	 */
	public static void main(final String[] args) {
		Runnable lift = new mainRun(args);
		lift.run();

	}
}

class mainRun implements Runnable {
	// this thread runs in the AWT event queue
	private String[] args;
	private static final int PTN = 0;
	private static final int OLD_TREE = 1;
	private static final int NEW_TREE = 2;
	private static final String GAF_SUFFIX = ".gaf";
	private String family_dir = "/Users/suzi/projects/go/gene-associations/submission/paint/";
	private String data_dir = "/Users/suzi/workspace/liftover/data/revisions";
	public final static String DESCENDANT_EVIDENCE_CODE = "IBD"; // was IDS

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AnnotatedAncestralNodeTransfers.class);

	public mainRun(String[] args) {
		this.args = args;
	}

	public void run() {
		List<String []> triplets;

		if (args[0].contains("p")) {
			String [] triplet = new String[3];
			triplet[0] = args[1];
			triplet[1] = args[2];
			triplet[2] = args[3];
			triplets = new ArrayList<>();
			triplets.add(triplet);
		} else {
			triplets = getTransferList();
		}
		int node_count = transfer(triplets);
		log.info("Transfered " + node_count + " PTN nodes");
	}

	private int transfer(List<String []> triplets) {
		log.info(triplets.size() + " PTN nodes to touch up");
		int count = 0;
		String today = dateNow();
		for (String [] triplet : triplets) {
			String old_dir = family_dir + triplet[OLD_TREE] + '/';
			boolean available = gafFileExists(old_dir, triplet[OLD_TREE]);
			if (!available) {
				log.info("Missing GAF file for " + triplet[OLD_TREE]);
			} else {
				GafDocument old_gaf = loadGAF(old_dir, triplet[OLD_TREE]);
				GafDocument new_gaf;
				if (gafFileExists(data_dir, triplet[NEW_TREE])) {
					new_gaf = loadGAF(data_dir, triplet[NEW_TREE]);
					log.info("Loading from already revised GAF for " + triplet[NEW_TREE]);
				} else {
					new_gaf = loadGAF(family_dir + triplet[NEW_TREE] + '/', triplet[NEW_TREE]);
					if (new_gaf == null) {
						String family_name = triplet[NEW_TREE];
						File gaf_dir = new File(data_dir);
						File gaf_file = new File(gaf_dir, family_name + ".gaf");
						log.info("Creating new GAF for " + gaf_file);
						new_gaf = new GafDocument(gaf_file.getAbsolutePath(), gaf_dir.getAbsolutePath());
					} else {
						log.info("Loading from original PAINT gaf for " + triplet[NEW_TREE]);
					}
				}
				if (new_gaf == null) {
					log.error("Something seriously wrong");
					System.exit(1);
				}
				if (old_gaf != null) {
					Collection<GeneAnnotation> ptn_annots = old_gaf.getGeneAnnotations("PANTHER:"+triplet[PTN]);
					boolean changed = false;
					for (Iterator<GeneAnnotation> i = ptn_annots.iterator(); i.hasNext();) {
						GeneAnnotation annot = i.next();
						List<String> refs = new ArrayList<String>();
						refs.add("PAINT_REF:"+triplet[NEW_TREE].substring(4));
						annot.setReferenceIds(refs);
						if (annot.getShortEvidence().equals(DESCENDANT_EVIDENCE_CODE)) { // was IDS
							new_gaf.addGeneAnnotation(annot);
							changed = true;
						} else if (annot.isDirectNot()) {
							new_gaf.addGeneAnnotation(annot);
							changed = true;
						}
					}
					if (changed) {
						recordGAF(new_gaf, triplet[NEW_TREE], 
								"redistribution of " + triplet[PTN] + " from " + triplet[OLD_TREE] + " to " + triplet[NEW_TREE],
								today);
						count++;
					}
				}
			}
		}
		return count;
	}

	private GafDocument loadGAF(String gaf_dir, String family_name) {
		File gaf_file = new File(gaf_dir, family_name + GAF_SUFFIX);
		GafDocument gafdoc = null;
		GafObjectsBuilder builder = new GafObjectsBuilder();
		try {
			gafdoc = builder.buildDocument(gaf_file.getAbsolutePath());
		} catch (IOException | URISyntaxException e) {
		}
		return gafdoc;
	}

	public void recordGAF(GafDocument new_gaf, String family_name, String comment, String today) {
		File gaf_dir = new File(data_dir);
		if (gaf_dir.isDirectory() && gaf_dir.canWrite()) {
			File gaf_file = new File(data_dir, family_name + GAF_SUFFIX);
			new_gaf.addComment("Lifted over by: " + comment + " on " + today);
			GafWriter gaf_writer = new GafWriter();
			gaf_writer.setStream(gaf_file);
			gaf_writer.write(new_gaf);
			IOUtils.closeQuietly(gaf_writer);
			log.info("Wrote updated GAF to " + gaf_file);
		} else {
			log.error("Unable to save GAF file for " + family_name + " in " + gaf_dir);
		}
	}

	private boolean gafFileExists(String family_dir, String family_name) {
		boolean ok;
		File gaf_file = new File(family_dir, family_name + GAF_SUFFIX);
		ok = !gaf_file.isDirectory() && gaf_file.canRead();
		return ok;
	}

	private List<String []> getTransferList() {
		ResourceLoader loader = ResourceLoader.inst();
		BufferedReader reader = loader.loadResource("PTN_forward_tracking.txt");

		List<String []> triplets = new ArrayList<>();

		if (reader != null) {
			try {
				String line = reader.readLine();
				while (line != null) {
					// allow for commenting out lines in the input file
					String [] columns = line.split("\t");
					String [] fixer = new String[3];
					fixer[PTN] = columns[PTN];
					fixer[OLD_TREE] = columns[OLD_TREE];
					fixer[NEW_TREE] = columns[NEW_TREE];
					triplets.add(fixer);
					line = reader.readLine();
				}
			} catch (Exception e) {
				log.warn("Could not load node list from PTN_forward_tracking.txt", e);
			}
		} else
			log.error("Could not load resource for term list: PTN_forward_tracking.txt");
		return triplets;
	}

	private String dateNow() {
		long timestamp = System.currentTimeMillis();
		/* Date appears to be fixed?? */
		Date when = new Date(timestamp);
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
		sdf.setTimeZone(TimeZone.getDefault()); // local time
		return sdf.format(when);
	}

}


