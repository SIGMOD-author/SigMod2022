clear all, close all, clc
load("pruneWithMask.mat");
%% For insertion
withoutPrune = pruneWithMask(1:4:4000);
withPruneQuery = pruneWithMask(2:4:4000);
withMask = pruneWithMask(4:4:4000);
f = figure;
h1 = area(withoutPrune);
h1.FaceColor = 'w';
hold on;
h2 = area(withMask);
h2.FaceColor = 'k';
%h3 = plot([1:1000], withPruneQuery);
legend("No pruning", "Pruning");
xlabel("Index size");
ylabel("Total number of OMD computation");
save_plot_as(f, "micro-prune-insertion");
%% For query
withoutPrune(2:1000) = withoutPrune(2:1000) - withoutPrune(1:999);
withPruneQuery(2:1000) = withPruneQuery(2:1000) - withPruneQuery(1:999);
f = figure;
plot([1:1000], withoutPrune, '--');
hold on;
plot([1:1000], withPruneQuery);
legend("No pruning", "Pruning");
xlabel("Index size");
ylabel("Number of OMD computation");
save_plot_as(f, "micro-prune-query");