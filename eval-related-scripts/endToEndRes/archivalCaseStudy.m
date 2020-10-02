%% Archival service case study
f = figure;
bar([1; 2; 26; 29.1])
xlabel("Query type")
ylabel("Ratio of hit SVSs (%)")
set(gca, 'XTickLabel', {'train', 'boat', 'fire hydrant', 'union'})
save_plot_as(f, 'end-to-end-archival')