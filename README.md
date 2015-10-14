# Things to do on new PANTHER release (incomplete)

## 1. Get new data

Using wget (see jenkins?), bring new data to geneontology/data/trees/panther SVN repository 
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

Need to know what's changed from the PANTHER side. Huaiyu will send two spreadsheets. 
First: is the list of PTNids that have changed families. These need to be a) moved from the GAF file in the old family to the GAF file in the new family, and 2) the updated GAFs have to be updated in the SVN repository

Second: is the list of families that are no longer in PANTHER. These need to be moved from the primary paint GAF directory in SVN to the "retired" subdirectory in SVN.

