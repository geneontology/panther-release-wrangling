# Things to do on new PANTHER release (incomplete)

## 1. Get new data

Job: https://build.berkeleybop.org/job/panther-update

Using wget, bring new data to geneontology/data/trees/panther SVN repository 
Then svn commit

We're currently using the following command:
```bash
## copy svn content from project sync-svn-cvs-trigger to cvs
# use wget
# -o Log all messages to panther-wgetlog
# -t Set number of attempts
# -N only retrieve files that are new (moot until panther site is returning time-stamps)
# -r recursive retrieval
# -np guarantees that only the files below a certain hierarchy will be downloaded
# -nH Disable generation of host-prefixed directories
# --cut-dirs This will not create the two intermediate directories "PANTHER9.0" and "books"
# -A accept files with these names
# -R reject files with these names
# -X exclude SF folders
# -P Local directory prefix
wget -q -S -N -r -nH -np --cut-dirs=2 -A attr.tab,tree.tree,tree.mia,cluster.wts -R cluster.fasta,cluster.ortholog,"hmm.*",cluster.pir,tree.sfan,index.html -X "/PANTHER10.0/books/*/SF*" -P panther data.pantherdb.org/PAINT_PANTHER10.0/books/
## commit to svn, set proper username and key file location for svn
#SVN_SSH="${global_svn_ssh_commit_line}" svn commit -m "v10 of PANTHER" panther/PTHR*/*
```

## 2. Talk to Huaiyu
Need to know what's changed from the PANTHER side. 

### 2.a Huaiyu is responsible for fixing all annotated ancestral node migration issues, specifically:
#### Annotated nodes that disappear completely
#### Annotated nodes that move to different tree (annotated already)
#### Annotated nodes that move to different tree (with no extant annotations)
#### Annotated nodes that move within trees and lose the original experimental annotations that were used as 'evidence'

### 2.b Relocate annotated PTNs
First: is the list of PTNids that have changed families. These need to be a) moved from the GAF file in the old family to the GAF file in the new family, and 2) the updated GAFs have to be updated in the SVN repository. See liftover code for the java app that does reads and writes the revised GAFs. The excel spreadsheet has to be slightly modified as text to be readable. 

### 2.c Deleted families
Second: is the list of families that are no longer in PANTHER. These need to be moved from the primary paint GAF directory in SVN to the "retired" subdirectory in SVN. Likewise in they need to be removed/retired from the data/trees/panther directory on SVN

## 3. Ensure taxonomic coverage
Make sure that all the taxa included in this PANTHER release are accounted for by the current taxon checker

## 4. Run touchup

## 5. Repair nhx and convert to JSON objects
Load into GOlr/Amigo

## 6. All the normal PAINT data handling

## 7. NEED all current Stanford scripts to be run by Jenkins!!!





