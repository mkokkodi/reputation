library(ggplot2)
library(grid)
results <- read.table(file="/Users/mkokkodi/git/reputation/data_results/real/results/synthetic_results.csv",head=TRUE,sep=",")
summary(results)

head(results)
mae_model_improvement<-(results$MAEBaseline -results$MAEModel)/results$MAEBaseline

improvements = transform(results,mae_improvement=mae_model_improvement)
head(improvements)

collabels = c("3 categories","5 categories","7 categories","Baseline")
#shapeLabels=c("Baseline","Hierarchical","Shrinkage")
ob1 <- ggplot(improvements,aes(HistoryThreshold,mae_improvement*100, colour=factor(categories),shape=exactModel))

ob1+geom_point(size=3)+facet_wrap(~model*approach * ScoreThreshold,ncol=3) + theme_bw(base_size = 18) +
  xlab(expression(History - eta))+
  ylab("Improvement %")+ 
  scale_colour_discrete(labels=collabels)+labs(colour="",shape="")+theme(legend.position = "right",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/synthetic.pdf",width=17,height=8,dpi=300)



hier <- read.table(file="/Users/mkokkodi/git/reputation/data_results/real/results/results.csv",head=TRUE,sep=",")
summary(hier)
mae_improvement<-(hier$MAEBaseline -hier$MAEModel )/hier$MAEBaseline
hier_improvements = transform(hier,improvement=mae_improvement )
hier_improvements
summary(hier_improvements)

ob1 <- ggplot(hier_improvements,aes(HistoryThreshold,mae_improvement*100, colour=factor(approach),shape=exactModel))

ob1+geom_point(size=3)+facet_wrap(~model* ScoreThreshold,ncol=3) + theme_bw(base_size = 18) +
  xlab(expression(History - eta))+
  ylab("Improvement %")+scale_shape_discrete(labels=shapeLabels)+ 
  labs(colour="",shape="")+theme(legend.position = "right",axis.title.y = element_text(vjust=-0.1),axis.title.x = element_text(vjust=-0.3),plot.margin = unit(c(1, 1, 1, 1), "cm"))

ggsave(file="/Users/mkokkodi/Dropbox/workspace/latex/reputation_informs/figures/syntheticHier.pdf",width=15,height=5,dpi=300)

