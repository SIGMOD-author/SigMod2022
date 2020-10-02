close all;
%% Average OMD comparison: SVS vs. Per-camera
f = figure;
h = bar([0.3, 0.31; 0.39, 0.82; 0.21, 0.8; 0.31, 0.7]);
h(1).FaceColor = "k";
h(2).FaceColor = "w";
xlabel("Camera type")
ylabel("Average OMD")
set(gca, 'XTickLabel', {'in-vehicle', 'harbor', 'train-station', 'combined'})
legend('SVS', 'Per-camera');
save_plot_as(f, 'micro-auto');
%% Case study: train-station, with train vs without train0
f2 = figure;
a = area([217, 60, 2, 2]./281 * 100);
a.FaceColor = "k";
a.FaceAlpha = 0.6;
hold on;
b = area([0, 110, 8, 4]./119 * 100);
b.FaceAlpha = 0.6;
b.FaceColor = "w";
legend('train-station-1', 'train-station-2')
set(gca, 'XTick', [1 2 3 4])
set(gca, 'XTickLabel', {'train', 'person', 'car', 'others'})
xlabel('Object type');
ylabel('Ratio in all observed objects (%)')
save_plot_as(f2, 'micro-auto-train');
%% Case study: Los angelos downtown
f3 = figure;
dis1 = [5103, 1397, 697, 119, 78];
dis2 = [3893, 1198, 889, 262, 237];
a = area(dis1/sum(dis1) * 100);
a.FaceAlpha = 0.6;
a.FaceColor = "k";
hold on;
b = area(dis2/sum(dis2) * 100);
b.FaceAlpha = 0.6;
b.FaceColor = "w";
set(gca, 'XTick', [1 2 3 4, 5])
set(gca, 'XTickLabel', {'car', 'person', 'traffic light', 'truck', 'others'})
xlabel('Object type');
ylabel('Ratio in all observed objects (%)')
legend('LA-1', 'LA-2')
save_plot_as(f3, 'micro-auto-la');