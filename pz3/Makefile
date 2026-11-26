TEX = xelatex
FLAGS = -interaction=nonstopmode -halt-on-error

.PHONY: all article clean

all: article

article:
	mkdir -p build
	$(TEX) $(FLAGS) -output-directory=build article.tex
	$(TEX) $(FLAGS) -output-directory=build article.tex
	@echo "PDF: build/article.pdf"

clean:
	rm -rf build
