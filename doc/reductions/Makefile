LATEX = pdflatex
BIBTEX = biber
LATEXFLAGS = -shell-escape
TEMPS = toc bbl blg log out lof lot lol aux pyg fdb_latexmk
ROOT = reductions

.PHONY: clean purge remake repeatedly
.SUFFIXES: .tex .pdf

.tex.pdf:
	$(LATEX) $(LATEXFLAGS) $<
	$(LATEX) $(LATEXFLAGS) $<

all: $(ROOT).pdf

clean:
	for s in $(TEMPS); do rm -f $(ROOT).$$s; done

purge: clean
	-rm -f $(ROOT).pdf

repeatedly:
	latexmk -pdf -pdflatex='pdflatex $(LATEXFLAGS) %O %S' -pvc $(ROOT).tex
