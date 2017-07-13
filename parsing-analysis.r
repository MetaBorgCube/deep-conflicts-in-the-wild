#!/usr/bin/env Rscript

library(plyr)
library(reshape2)
library(functional)
library(extrafont)
library(scales)
loadfonts()

# setwd("~/repos/decl-disamb/deep-conflicts-in-the-wild")
rm(list = ls())
cat("\014")

args <- commandArgs(trailingOnly = TRUE)

# args <- c("Disamb-Experiment/logs/",
          # "ocaml")

curWD <- getwd()
workingDir <- paste(curWD, args[1], sep = "/")

if (exists("workingDir")) {
  setwd(workingDir)
} else {
  print(paste(workingDir, "has not been found.", sep = " "))
}

prefix  <- args[2]
print(prefix)
dataAll <-
  tryCatch({
    read.csv(
      paste(prefix, "statistics.txt", sep = "-"),
      sep = ";",
      header = FALSE,
      stringsAsFactors = FALSE
    )
  }, error = function(e) {
    return(
      data.frame(
        filename = character(),
        linesOfCode = character(),
        astNodes = character(),
        visibleStates = character(),
        processedStates = character(),
        productionsUsed = character(),
        ambiguitiesOS = character(),
        ambiguitiesDE = character(),
        ambiguitiesLM = character(),
        uniqueAmbNodesOS = character(),
        uniqueAmbNodesDE = character(),
        uniqueAmbNodesLM = character(),
        bracketsDeep = character(),
        bracketsShallow = character(),
        bracketsReadability = character(),
        extraCol = character(),
        stringsAsFactors = FALSE
      )
    )
  })

# drop empty row due to trailing semicolon
dataAll <- dataAll[-ncol(dataAll)]

colnames(dataAll) <-
  c(
    "filename",
    "linesOfCode",
    "astNodes",
    "visibleStates",
    "processedStates",
    "productionsUsed",
    "ambiguitiesOS",
    "ambiguitiesDE",
    "ambiguitiesLM",
    "uniqueAmbNodesOS",
    "uniqueAmbNodesDE",
    "uniqueAmbNodesLM",
    "bracketsDeep",
    "bracketsShallow",
    "bracketsReadability"
  )

dataExtensionValid <-
  tryCatch({
    read.csv(
      paste(prefix, "mapping.txt", sep = "-"),
      sep = ";",
      header = FALSE,
      stringsAsFactors = FALSE
    )
  },
  error = function(e) {
    return(
      data.frame(
        language = character(),
        project = character(),
        path = character(),
        stringsAsFactors = FALSE
      )
    )
  })


colnames(dataExtensionValid) <- c("language", "project", "path")

# overwrite language with prefix
if (nrow(dataExtensionValid) != 0) {
  dataExtensionValid$language <- prefix
  dataExtensionValid$language <-
    as.factor(dataExtensionValid$language)
  dataExtensionValid$project <-
    as.factor(dataExtensionValid$project)
}

dataExtensionFailed <-
  tryCatch({
    read.csv(
      paste(prefix, "failing-mapping.txt", sep = "-"),
      sep = ";",
      header = FALSE,
      stringsAsFactors = FALSE
    )
  }, error = function(e) {
    return(
      data.frame(
        language = character(),
        project = character(),
        path = character(),
        stringsAsFactors = FALSE
      )
    )
  })

colnames(dataExtensionFailed) <- c("language", "project", "path")

if (nrow(dataExtensionFailed) != 0) {
  # overwrite language with prefix
  dataExtensionFailed$language <- prefix
  dataExtensionFailed$language <-
    as.factor(dataExtensionFailed$language)
  dataExtensionFailed$project <-
    as.factor(dataExtensionFailed$project)
}

capitalize <- function(s)
  paste(toupper(substring(s, 1, 1)), {
    s <- substring(s, 2)
    s
  }, sep = "", collapse = " ")

# curried with `prefix`
macroName__ <- function(columnName) {
  paste(prefix, capitalize(columnName), sep = '')
}

macroName <- Vectorize(macroName__)

# curried without currying `prefix`
macroName2__ <- function(prefix, columnName) {
  paste(prefix, capitalize(columnName), sep = '')
}

macroName2 <- Vectorize(macroName2__)



# will store what we're going to talk about in the paper
paperResultVariables <- new.env()



nrowStatisticsFullGrammar <- nrow(dataAll) - 7

# retrieve summary at the end and then remove row
paperResultVariables[[macroName("fullGrammarProductions")]]               <-
  as.numeric(as.character(dataAll[nrowStatisticsFullGrammar, 1]))
paperResultVariables[[macroName("fullGrammarProductionsUsed")]]           <-
  as.numeric(as.character(dataAll[nrowStatisticsFullGrammar, 5]))
paperResultVariables[[macroName("fullGrammarProductionsUsedPercentage")]] <-
  round((paperResultVariables[[macroName("fullGrammarProductionsUsed")]] / paperResultVariables[[macroName("fullGrammarProductions")]]) * 100, 1)
paperResultVariables[[macroName("fullGrammarStates")]]                    <-
  as.numeric(as.character(dataAll[nrowStatisticsFullGrammar, 2]))
paperResultVariables[[macroName("fullGrammarStatesVisible")]]             <-
  as.numeric(as.character(dataAll[nrowStatisticsFullGrammar, 3]))
paperResultVariables[[macroName("fullGrammarStatesVisiblePercentage")]]   <-
  round((paperResultVariables[[macroName("fullGrammarStatesVisible")]]   / paperResultVariables[[macroName("fullGrammarStates")]]) * 100, 1)
paperResultVariables[[macroName("fullGrammarStatesProcessed")]]           <-
  as.numeric(as.character(dataAll[nrowStatisticsFullGrammar, 4]))
paperResultVariables[[macroName("fullGrammarStatesProcessedPercentage")]] <-
  round((paperResultVariables[[macroName("fullGrammarStatesProcessed")]] / paperResultVariables[[macroName("fullGrammarStates")]]) * 100, 1)

if (nrow(dataAll) == 0) {
  print(paste("No data for language", prefix, sep = " "))
  q('no')
}

# retrieve individual grammar statistics
grammarStatistics <-
  dataAll[(nrow(dataAll) - 7):nrow(dataAll), 1:2]
rownames(grammarStatistics) <-
  macroName(
    c(
      "grammarFull",
      "grammarDeLm",
      "grammarLmOs",
      "grammarDeOs",
      "grammarOs",
      "grammarDe",
      "grammarLm",
      "grammarBase"
    )
  )
colnames(grammarStatistics) <- c("productions", "states")
#
# clean data set
dataAll <- dataAll[-(nrowStatisticsFullGrammar:nrow(dataAll)),]

# select subset of data where last column != NA
dataValid <- dataAll[is.na(dataAll[, ncol(dataAll)]) == FALSE,]
# clean data set
dataValid$astNodes <- as.numeric(as.character(dataValid$astNodes))
# extend with project info
dataValid <- cbind(dataValid, dataExtensionValid)



if (nrow(dataValid) != 0) {
  dataValid$fileCount <- 1
} else {
  dataValid$fileCount <- numeric(0)
}

# select subset of data where last column == NA
dataFailed <- dataAll[is.na(dataAll[, ncol(dataAll)]) == TRUE, ]
# clean data set
names(dataFailed)[3] <- "reasonForFailure"
dataFailed <- dataFailed[, 1:3]
# extend with project info
# dataFailed <- cbind(dataFailed, dataExtensionFailed)
#

if (nrow(dataFailed) != 0) {
  dataFailed$fileCount <- 1
} else {
  dataFailed$fileCount <- numeric(0)
}



paperResultVariables[[macroName("dataAll")]]    <- nrow(dataAll)
paperResultVariables[[macroName("dataValid")]]  <- nrow(dataValid)
paperResultVariables[[macroName("dataFailed")]] <-
  nrow(dataFailed)

paperResultVariables[[macroName("dataValidPercentage")]]  <-
  round((nrow(dataValid)  / nrow(dataAll)) * 100, 1)
paperResultVariables[[macroName("dataFailedPercentage")]] <-
  round((nrow(dataFailed) / nrow(dataAll)) * 100, 1)

percentageOfFailedDataPoints <- nrow(dataFailed) / nrow(dataAll)
#OUTPUT# round(percentageOfFailedDataPoints * 100, 2)
# TODO: explain different failure categories

# summarizing ambiguities (for fitlerting and statistics)
dataValid$ambiguities       <-
  dataValid$ambiguitiesOS + dataValid$ambiguitiesDE + dataValid$ambiguitiesLM
dataValid$uniqueAmbNodes    <-
  dataValid$uniqueAmbNodesOS + dataValid$uniqueAmbNodesDE + dataValid$uniqueAmbNodesLM
dataValid$brackets          <-
  dataValid$bracketsDeep + dataValid$bracketsShallow + dataValid$bracketsReadability
dataValidWithoutAmbiguities <-
  dataValid[dataValid$ambiguities == 0, ]
dataValidWithAmbiguities    <-
  dataValid[dataValid$ambiguities != 0, ]

paperResultVariables[[macroName("dataValidWithAmbiguities")]]    <-
  nrow(dataValidWithAmbiguities)
paperResultVariables[[macroName("dataValidWithoutAmbiguities")]] <-
  nrow(dataValidWithoutAmbiguities)
paperResultVariables[[macroName("percentageOfDataPointsWithAmbiguities")]] <-
  round((nrow(dataValidWithAmbiguities) / nrow(dataValid)) * 100, 1)
#OUTPUT# View(dataValidWithAmbiguities[c("filename", "ambiguitiesOS", "ambiguitiesDE", "ambiguitiesLM", "ambiguities")])
#OUTPUT# round(percentageOfProgramsWithAmbiguities * 100, 2)

subsetAmbiguityColumns <-
  dataValid[c("ambiguitiesOS", "ambiguitiesDE", "ambiguitiesLM")]
#
colSums(subsetAmbiguityColumns)

if (nrow(subsetAmbiguityColumns) > 1) {
  colSums(sapply(subsetAmbiguityColumns, Vectorize(function (x) {
    if (x == 0) {
      x
    } else {
      x / x
    }
  })), na.rm = F)
  
  ftable(rowSums(sapply(
    subsetAmbiguityColumns, Vectorize(function (x) {
      if (x == 0) {
        x
      } else {
        x / x
      }
    })
  ), na.rm = F))  
  
  round((ftable(rowSums(
    sapply(subsetAmbiguityColumns, Vectorize(function (x) {
      if (x == 0) {
        x
      } else {
        x / x
      }
    })), na.rm = F
  )) / nrow(dataValidWithAmbiguities)) * 100, 1)
}


subsetBracketColumns <-
  dataValid[c("bracketsDeep", "bracketsShallow", "bracketsReadability")]
#
colSums(subsetBracketColumns)
#
bracketsTotal <- sum(colSums(subsetBracketColumns))
round((colSums(subsetBracketColumns) / bracketsTotal) * 100, 1)


fullGrammarProductions <-
  paperResultVariables[[macroName("fullGrammarProductions")]]
fullGrammarStates      <-
  paperResultVariables[[macroName("fullGrammarStates")]]

# numberOfProcessedStates
# boxplot(dataValidWithoutAmbiguities$processedStates, dataValidWithAmbiguities$processedStates)
# percentOfProcessedStates

print(getwd())
outputFolder <- "."
fontScalingFactor <- 1.1

outputFileName <-
  paste(paste(
    prefix,
    capitalize("boxplot"),
    capitalize("grammar"),
    capitalize("coverageWithAndWithoutDeepConflicts"),
    sep = ""
  ),
  "pdf",
  sep = ".")
pdf(
  paste(outputFolder, outputFileName, sep = "/"),
  outputFileName,
  family = "Times",
  width = 6.5,
  height = 5.5
)
par(mar = c(2.3, 4.5, 0, 4.3) + 0.15) # c(bottom, left, top, right)
boxplot((
  dataValidWithoutAmbiguities$productionsUsed * 100 / fullGrammarProductions
),
(
  dataValidWithAmbiguities$productionsUsed * 100 / fullGrammarProductions
),
outline = T,
horizontal = F,
xaxt = "n",
ylim = range(0, 40),
ylab = "Contextual Grammar Production Coverage (in %)",
cex.lab = fontScalingFactor,
cex.axis = fontScalingFactor,
cex.main = fontScalingFactor,
cex.sub = fontScalingFactor
)
axis(
  side = 1,
  at = c(1, 2),
  labels = c(
    "Programs without\nDeep Priority Conflics",
    "Programs with\nDeep Priority Conflicts"
  ),
  tick = F,
  cex.lab = fontScalingFactor,
  cex.axis = fontScalingFactor,
  cex.main = fontScalingFactor,
  cex.sub = fontScalingFactor
)
axis(
  side = 2,
  at = c(0, 5, 10, 15, 20, 25, 30, 35, 40),
  cex.lab = fontScalingFactor,
  cex.axis = fontScalingFactor,
  cex.main = fontScalingFactor,
  cex.sub = fontScalingFactor
)
dev.off()
embed_fonts(outputFileName)

outputFileName <-
  paste(paste(
    prefix,
    capitalize("boxplot"),
    capitalize("parseTable"),
    capitalize("coverageWithAndWithoutDeepConflicts"),
    sep = ""
  ),
  "pdf",
  sep = ".")
pdf(
  paste(outputFolder, outputFileName, sep = "/"),
  outputFileName,
  family = "Times",
  width = 6.5,
  height = 5.5
)
par(mar = c(2.3, 4.5, 0, 4.3) + 0.15) # c(bottom, left, top, right)
boxplot((
  dataValidWithoutAmbiguities$processedStates * 100 / fullGrammarStates
),
(
  dataValidWithAmbiguities$processedStates * 100 / fullGrammarStates
),
outline = T,
horizontal = F,
xaxt = "n",
ylim = range(0, 40),
ylab = "Processed States of Lazy Parse Table (in %)",
cex.lab = fontScalingFactor,
cex.axis = fontScalingFactor,
cex.main = fontScalingFactor,
cex.sub = fontScalingFactor
)
axis(
  side = 1,
  at = c(1, 2),
  labels = c(
    "Programs without\nDeep Priority Conflics",
    "Programs with\nDeep Priority Conflicts"
  ),
  tick = F,
  cex.lab = fontScalingFactor,
  cex.axis = fontScalingFactor,
  cex.main = fontScalingFactor,
  cex.sub = fontScalingFactor
)
axis(
  side = 2,
  at = c(0, 5, 10, 15, 20, 25, 30, 35, 40),
  cex.lab = fontScalingFactor,
  cex.axis = fontScalingFactor,
  cex.main = fontScalingFactor,
  cex.sub = fontScalingFactor
)
dev.off()
embed_fonts(outputFileName)



paperResultVariables[[macroName("individualGrammarProductionsUsedPercentageMin")]]  <-
  round(min (dataValid$productionsUsed) * 100 / fullGrammarProductions,
        1)
paperResultVariables[[macroName("individualGrammarProductionsUsedPercentageMean")]] <-
  round(mean(dataValid$productionsUsed) * 100 / fullGrammarProductions,
        1)
paperResultVariables[[macroName("individualGrammarProductionsUsedPercentageMax")]]  <-
  round(max (dataValid$productionsUsed) * 100 / fullGrammarProductions,
        1)
#
round(min (dataValid$processedStates) * 100 / fullGrammarStates, 1)
round(mean(dataValid$processedStates) * 100 / fullGrammarStates, 1)
round(max (dataValid$processedStates) * 100 / fullGrammarStates, 1)

min (dataValidWithAmbiguities$processedStates) * 100 / fullGrammarStates
mean(dataValidWithAmbiguities$processedStates) * 100 / fullGrammarStates
max (dataValidWithAmbiguities$processedStates) * 100 / fullGrammarStates

min (dataValidWithoutAmbiguities$processedStates) * 100 / fullGrammarStates
mean(dataValidWithoutAmbiguities$processedStates) * 100 / fullGrammarStates
max (dataValidWithoutAmbiguities$processedStates) * 100 / fullGrammarStates



#   dss_stats_meltByElementCount <- melt(dss_stats, id.vars=c('Param_size', 'Param_valueFactoryFactory', 'Param_dataType', 'arch'), measure.vars=c('footprintInBytes'))
#   dss_stats_castByMedian <- dcast(dss_stats_meltByElementCount, Param_size + Param_valueFactoryFactory + Param_dataType ~ paste("footprintInBytes", arch, "median", sep = "_"), median, fill=0)

formatPercent__ <- function(arg, rounding = F) {
  if (is.nan(arg)) {
    x <- "0"
  } else {
    argTimes100 <- as.numeric(arg) * 100
    digits = 1
    
    if (rounding == T) {
      x <-
        format(
          round(argTimes100, digits),
          nsmall = digits,
          digits = digits,
          scientific = FALSE
        )
    } else {
      x <-
        format(
          argTimes100,
          nsmall = digits,
          digits = digits,
          scientific = FALSE
        )
    }
  }
  
  x <- paste(x, "\\%", sep = "")
  x
}
formatPercent <- Vectorize(formatPercent__)

normalize <-
  Vectorize(function (x) {
    if (x == 0) {
      0
    } else {
      as.integer(x / x)
    }
  })
# sumNormalized <- function (input) sum(sapply(input, normalize))

ftable(dataValid$project)
rowSums(ftable(dataValidWithAmbiguities$project))

fileCount <-
  as.integer(as.data.frame(t(as.matrix(
    ftable(dataValid$project)
  )))$V1)
# colnames(fileCount) <- c("fileCount")

str(dataValid)
m <-
  melt(
    dataValid,
    id.vars = c("project"),
    measure.vars = c(
      "ambiguitiesOS",
      "ambiguitiesDE",
      "ambiguitiesLM",
      "ambiguities"
    )
  )
m$variable <- macroName2("perFile", m$variable)
m$value <- normalize(m$value)
str(m)
d <- dcast(m, project ~ variable, sum)
d$project <- as.character(d$project)
d$fileCount <- as.numeric(fileCount)
# d <- rbind(d, "All" <- colSums(d[,2:ncol(d)]))
d

# dFmt$perFileAmbiguitiesPercent   <- dFmt$perFileAmbiguities   / dFmt$fileCount
# dFmt$perFileAmbiguitiesOSPercent <- dFmt$perFileAmbiguitiesOS / dFmt$fileCount
# dFmt$perFileAmbiguitiesDEPercent <- dFmt$perFileAmbiguitiesDE / dFmt$fileCount
# dFmt$perFileAmbiguitiesLMPercent <- dFmt$perFileAmbiguitiesLM / dFmt$fileCount


m2 <-
  melt(
    dataValid,
    id.vars = c("project"),
    measure.vars = c(
      "fileCount",
      "ambiguitiesOS",
      "ambiguitiesDE",
      "ambiguitiesLM",
      "ambiguities",
      "bracketsDeep",
      "bracketsShallow",
      "bracketsReadability",
      "brackets"
    )
  )
d2 <- dcast(m2, project ~ variable, sum)
str(d2)

tableData        <- join(d, d2)
tableDataSummary <-
  tail(rbind(tableData, c("All", colSums(tableData[, 2:ncol(tableData)]))), 1)

formatResultTable <- function(tableData) {
  dFmt <- data.frame(tableData)
  
  dFmt$empty1 <- ""
  dFmt$empty2 <- ""
  dFmt$empty3 <- ""
  
  dFmt$perFileAmbiguitiesPercent   <-
    as.numeric(dFmt$perFileAmbiguities)   / as.numeric(dFmt$fileCount)
  dFmt$perFileAmbiguitiesOSPercent <-
    as.numeric(dFmt$perFileAmbiguitiesOS) / as.numeric(dFmt$fileCount)
  dFmt$perFileAmbiguitiesDEPercent <-
    as.numeric(dFmt$perFileAmbiguitiesDE) / as.numeric(dFmt$fileCount)
  dFmt$perFileAmbiguitiesLMPercent <-
    as.numeric(dFmt$perFileAmbiguitiesLM) / as.numeric(dFmt$fileCount)
  
  dFmt$ambiguitiesOSPercent <-
    formatPercent(as.numeric(dFmt$ambiguitiesOS) / as.numeric(dFmt$ambiguities))
  dFmt$ambiguitiesDEPercent <-
    formatPercent(as.numeric(dFmt$ambiguitiesDE) / as.numeric(dFmt$ambiguities))
  dFmt$ambiguitiesLMPercent <-
    formatPercent(as.numeric(dFmt$ambiguitiesLM) / as.numeric(dFmt$ambiguities))
  
  dFmt$bracketsDeepAndShallow         <-
    as.numeric(dFmt$bracketsDeep) + as.numeric(dFmt$bracketsShallow)
  dFmt$bracketsDeepAndShallowPercent  <-
    formatPercent(as.numeric(dFmt$bracketsDeepAndShallow) / as.numeric(dFmt$brackets))
  dFmt$bracketsDisambiguation         <-
    sprintf("%s (%s)",
            dFmt$bracketsDeepAndShallow,
            dFmt$bracketsDeepAndShallowPercent)
  
  dFmt$bracketsDeepPercent        <-
    sprintf(
      "%s (%s)",
      as.numeric(dFmt$bracketsDeep),
      formatPercent(as.numeric(dFmt$bracketsDeep)        / as.numeric(dFmt$brackets))
    )
  dFmt$bracketsShallowPercent     <-
    sprintf(
      "%s (%s)",
      as.numeric(dFmt$bracketsShallow),
      formatPercent(
        as.numeric(dFmt$bracketsShallow)     / as.numeric(dFmt$brackets)
      )
    )
  dFmt$bracketsReadabilityPercent <-
    formatPercent(as.numeric(dFmt$bracketsReadability) / as.numeric(dFmt$brackets))
  
  sapply(2:ncol(dFmt), function(col_idx) {
    dFmt[, c(col_idx)] <- as.character(dFmt[, c(col_idx)])
  })
  dFmt$perFileAmbiguitiesPercent   <-
    formatPercent(dFmt$perFileAmbiguitiesPercent)
  dFmt$perFileAmbiguitiesOSPercent <-
    formatPercent(dFmt$perFileAmbiguitiesOSPercent)
  dFmt$perFileAmbiguitiesDEPercent <-
    formatPercent(dFmt$perFileAmbiguitiesDEPercent)
  dFmt$perFileAmbiguitiesLMPercent <-
    formatPercent(dFmt$perFileAmbiguitiesLMPercent)
  dFmt$affectedFiles <-
    sprintf(
      "%s / %s (%s)",
      dFmt$perFileAmbiguities,
      dFmt$fileCount,
      dFmt$perFileAmbiguitiesPercent
    )
  
  dFmtColumnNames <- c(
    "project"
    # , "fileCount"
    # , "perFileAmbiguities"
    # , "perFileAmbiguitiesPercent"
    ,
    "affectedFiles"
    ,
    "empty1"
    ,
    "ambiguities"
    # , "ambiguitiesOS"
    ,
    "ambiguitiesOSPercent"
    # , "ambiguitiesDE"
    ,
    "ambiguitiesDEPercent"
    # , "ambiguitiesLM"
    ,
    "ambiguitiesLMPercent"
    # , "perFileAmbiguitiesOS"
    # , "perFileAmbiguitiesOSPercent"
    # , "perFileAmbiguitiesDE"
    # , "perFileAmbiguitiesDEPercent"
    # , "perFileAmbiguitiesLM"
    # , "perFileAmbiguitiesLMPercent"
    # , "brackets"
    # , "bracketsDeepAndShallow"
    # , "bracketsDisambiguation"
    # , "bracketsDeep"
    # , "bracketsShallow"
    # , "bracketsReadability"
    ,
    "empty2"
    ,
    "bracketsDeepPercent"
    ,
    "bracketsShallowPercent"
    # , "bracketsReadabilityPercent"
  )
  
  dFmt <- dFmt[, dFmtColumnNames]
  # View(dFmt)
  dFmt
}

# convert environment to data frame
paperResultDataFrame <-
  as.data.frame(as.list(paperResultVariables))
paperResultTexMacros <-
  sprintf(
    "\\newcommand{\\%s}{%s}\n",
    names(paperResultDataFrame),
    paperResultDataFrame
  )

print(paperResultTexMacros)

# write to file
outputFolder <- "."
outputFileName <-
  paste(outputFolder,
        paste("evaluationResultTexMacros", prefix, ".tex", sep = ""),
        sep = "/")
#
sink(outputFileName)
cat(paste(paperResultTexMacros, collapse = ''))
sink()



fileName <-
  paste(outputFolder, paste(prefix, capitalize("resultTable"), ".tex", sep =
                              ""), sep = "/")
write.table(
  formatResultTable(tableData),
  file = fileName,
  sep = " & ",
  row.names = FALSE,
  col.names = FALSE,
  append = FALSE,
  quote = FALSE,
  eol = " \\\\ \n"
)

fileName <-
  paste(outputFolder, paste(prefix, capitalize("resultTableSummary"), ".tex", sep =
                              ""), sep = "/")
write.table(
  formatResultTable(tableDataSummary),
  file = fileName,
  sep = " & ",
  row.names = FALSE,
  col.names = FALSE,
  append = FALSE,
  quote = FALSE,
  eol = " \\\\ \n"
)
