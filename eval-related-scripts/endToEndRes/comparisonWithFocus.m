%% average query time
f = figure;
h = bar([1,1.03; 1,1.02; 1,0.98]);
h(1).FaceColor = 'k';
h(2).FaceColor = 'w';
ylim([0,1.08]);
xlabel("Query type")
ylabel("Normalized query time")
set(gca, 'XTickLabel', {'fire hydrant', 'boat', 'train'})
legend('Video-zilla', 'Focus')
save_plot_as(f, 'end-to-end-focus-average')
%% accumulated GPU time
f = figure;
h = bar([1,1.7; 1, 7.1; 1, 13.8]);
h(1).FaceColor = 'k';
h(2).FaceColor = 'w';
xlabel("Query type")
ylabel("Normalized accumulated GPU time")
set(gca, 'XTickLabel', {'fire hydrant', 'boat', 'train'})
legend('Video-zilla', 'Focus')
save_plot_as(f, 'end-to-end-focus-accumulated')
%% illustraction figure of the noisy information
f = figure;
hb = bar([5103, 1497, 697, 320]/sum([5103, 1497, 697, 320]) * 100);
xlabel("Cluster type");
ylabel("Ratio of elements (%) ");
set(gca, 'XTickLabel', {'C1', 'C2', 'C3', 'C4'});
hb(1).FaceColor = 'y';
ylim([0,80]);
save_plot_as(f, 'end-to-end-focus-illus-video-zilla')
f2 = figure;
hb = bar([5308, 1297, 670, 420, 50]/sum([5308, 1297, 670, 420, 50]) * 100);
hb(1).FaceColor = 'g';
xlabel("Object type");
ylabel("Ratio of elements (%) ");
set(gca, 'XTickLabel', {'Car', 'Light', 'Person', 'Truck', 'Other'});
ylim([0,80]);
save_plot_as(f2, 'end-to-end-focus-illus-focus')
