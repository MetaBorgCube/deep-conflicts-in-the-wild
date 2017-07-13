#!/bin/bash

if [ $# -ne 1 ]; then
  echo "Error. No directory containing the logs passed. Please run ./gen-pdf.sh <Logs-folder>"
  exit 1
fi 

if [ ! -d $1 ]; then 
  echo "Error. No directory found."
  exit 1
fi

if [[ "$1" != */ ]]
then
    WORKING_DIR="$1/"
else
    WORKING_DIR=$1
fi

export REGEX='s/^test\/\([-[:alnum:]]*\)\/\([-[:alnum:]]*\)\/\([[:print:]]*\)$/\1\;\2\;\3/g'

for f in $WORKING_DIR*-files.txt
do 
   filename=$(basename "$f")
   filename="${filename%-files.*}"
   cat $f | sed $REGEX | less > $WORKING_DIR$filename-mapping.txt
done 

OUTPUT_DIR="$WORKING_DIR"pdfs/

if [ ! -d $OUTPUT_DIR ]; then 
  mkdir $OUTPUT_DIR
fi 


COVERAGE_TABLE="
\documentclass[10pt]{article}
  \usepackage{multirow}
  \usepackage[margin=2in]{geometry}
  \usepackage{tabularx}
  \newcolumntype{R}{>{\raggedleft\arraybackslash}X}%
  \newcolumntype{C}{>{\centering\arraybackslash}X}%

  % Tables
  \newcommand{\tableheader}[1]{\textbf{#1}}%
  \newcommand{\tablefooter}[1]{\textbf{#1}}%

  % http://www.inf.ethz.ch/personal/markusp/teaching/guides/guide-tables.pdf
  \usepackage{booktabs}
  \newcommand{\ra}[1]{\renewcommand{\arraystretch}{#1}}

  %% Some recommended packages.
  \usepackage{booktabs}   %% For formal tables:
                        %% http://ctan.org/pkg/booktabs
"

echo "$COVERAGE_TABLE" > ${OUTPUT_DIR}coverage-statistics.tex

for f in $WORKING_DIR*-statistics.txt
do
  filename=$(basename "$f")
  LANG="${filename%-statistics.txt}"

  Rscript parsing-analysis.r $WORKING_DIR $LANG

  RESULT_TABLE=$(cat $WORKING_DIR${LANG}ResultTable.tex)
  RESULT_TABLE_SUMMARY=$(cat $WORKING_DIR${LANG}ResultTableSummary.tex)

  if [ ! -f $WORKING_DIR${LANG}ResultTable.tex ]; then
    continue
  fi

  MACROS=$(cat ${WORKING_DIR}evaluationResultTexMacros$LANG.tex)

  echo "$MACROS" >> ${OUTPUT_DIR}coverage-statistics.tex

  TABLE_LATEX="

  \documentclass[10pt]{article}
  \usepackage{multirow}
  \usepackage[margin=0.5in]{geometry}
  \usepackage{tabularx}
  \newcolumntype{R}{>{\raggedleft\arraybackslash}X}%
  \newcolumntype{C}{>{\centering\arraybackslash}X}%

  % Tables
  \newcommand{\tableheader}[1]{\textbf{#1}}%
  \newcommand{\tablefooter}[1]{\textbf{#1}}%

  % http://www.inf.ethz.ch/personal/markusp/teaching/guides/guide-tables.pdf
  \usepackage{booktabs}
  \newcommand{\ra}[1]{\renewcommand{\arraystretch}{#1}}

  %% Some recommended packages.
  \usepackage{booktabs}   %% For formal tables:
                        %% http://ctan.org/pkg/booktabs

  \begin{document}

  \begin{table*}
    \caption{Overview of Deep Priority Conflicts and Bracket Usage in $LANG Corpus.}
      \small
      \ra{1.4}
      \begin{tabularx}{1.00\textwidth}{@{\hspace*{2pt}}rrr*{4}{R}rrr}
        \toprule
  \multirow{2}{*}{\tableheader{Project}}
  & \multirow{2}{*}{\tableheader{Affected Files}}
  & \multirow{2}{*}{}
  & \multicolumn{4}{c}{\tableheader{Deep Priority Conflicts}}
  & \multirow{2}{*}{}
  & \multicolumn{2}{c}{\tableheader{Disambiguation with Brackets}}
  \\\\

  \cmidrule(lr){4-7}
  \cmidrule(lr){9-10}

  &
  &
  & \tableheader{Total Number}
  & \tableheader{Operator Style}
  & \tableheader{Dangling Else}
  & \tableheader{Longest Match}
  &
  & \tableheader{Deep Conflicts}
  & \tableheader{Shallow Conflicts}
  \\\\
        \midrule
          $RESULT_TABLE
        \midrule
          $RESULT_TABLE_SUMMARY
        \bottomrule
      \end{tabularx}
  \label{tbl:$LANG-results}
  \end{table*}


  \end{document}"

  echo "$TABLE_LATEX" > $OUTPUT_DIR$LANG-table.txt

  pdflatex -output-directory $OUTPUT_DIR $OUTPUT_DIR$LANG-table.txt
  
done

echo "\begin{document}

  \begin{table}
    \caption{Grammar and Parse Table Coverage Statistics.}%
      \small
      %\footnotesize
      \ra{1.4}%
      \begin{tabularx}{1.00\columnwidth}{@{\hspace*{2pt}}rCrcCrrr}%
        \toprule%
  \multirow{3}{*}{} % \tableheader{Language}
& \multirow{3}{*}{}
& \multicolumn{2}{c}{\tableheader{Grammar}}
& \multirow{3}{*}{}
& \multicolumn{3}{c}{\tableheader{Parse Table}}
\\\\

\cmidrule(lr){3-4}%
\cmidrule(l ){6-8}%

&
& \tableheader{\# Prod.}
& \tableheader{Used}
&
& \tableheader{\# States}
& \multicolumn{2}{c}{\tableheader{Lazy Expansion}}
%& \tableheader{Proc.}
%& \tableheader{Visible}
\\\\

\cmidrule(l ){7-8}%

&
&
&
&
&
& \tableheader{~~Proc.}
& \tableheader{Visible}
\\\\

        \midrule%
" >> ${OUTPUT_DIR}coverage-statistics.tex

for f in $WORKING_DIR*-statistics.txt
do
  filename=$(basename "$f")
  LANG="${filename%-statistics.txt}"
  

  if [ ! -f ${WORKING_DIR}evaluationResultTexMacros$LANG.tex ]; then
    continue
  fi

  echo "$LANG & 
& \\${LANG}FullGrammarProductions & \\${LANG}FullGrammarProductionsUsedPercentage\% &
& \\${LANG}FullGrammarStates & \\${LANG}FullGrammarStatesProcessedPercentage\% & \\${LANG}FullGrammarStatesVisiblePercentage\% \\\\" >> ${OUTPUT_DIR}coverage-statistics.tex

done

echo "        \bottomrule%
      \end{tabularx}%
\label{tbl:grammar-and-parse-table-statistics}      
\end{table}

  \end{document}" >> ${OUTPUT_DIR}coverage-statistics.tex


pdflatex -output-directory $OUTPUT_DIR ${OUTPUT_DIR}coverage-statistics.tex

rm $OUTPUT_DIR*.txt
rm $OUTPUT_DIR*.log
rm $OUTPUT_DIR*.aux
rm $WORKING_DIR*.tex
rm $OUTPUT_DIR*.tex
mv $WORKING_DIR*.pdf $OUTPUT_DIR
rm $WORKING_DIR*-mapping.txt
rm *.Rout
