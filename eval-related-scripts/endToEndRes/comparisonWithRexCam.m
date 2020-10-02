%% Accumulated & Recall plot
close all;
f = figure;
yyaxis left
hb = bar([1, 4.2; 1, 1.8; 1, 1.9]);
xlabel("Query type")
ylabel("Normalized accumulated GPU time", 'FontSize', 6')
hold on;
set(gca, 'XTickLabel', {'fire hydrant', 'boat', 'train'})
legend("RexCam", "Video-zilla")
hb(1).FaceColor = 'k';
hb(2).FaceColor = 'w';
yyaxis right
ylabel("Recall(%)")
plot([1,2,3], [30, 60, 56], 'b--');
plot([1,2,3], [96, 95, 95.5], 'r-.');
ylim([0,100])
legend("RexCam", "Video-zilla", "RexCam", "Video-zilla")
save_plot_as_rexcam(f, "end-to-end-rexcam")
%% Precision & recall plot
f = figure;
yyaxis left
hb = bar([96.1 95.1; 95, 96; 94.1, 95.6]);
xlabel("Query type")
ylabel("Precision (%)")
hold on;
set(gca, 'XTickLabel', {'fire hydrant', 'boat', 'train'})
legend("RexCam", "Video-zilla")
hb(1).FaceColor = 'k';
hb(2).FaceColor = 'w';
yyaxis right
ylabel("Recall(%)")
plot([1,2,3], [30, 60, 56], 'b-s');
plot([1,2,3], [96, 95, 95.5], 'r-o');
ylim([0,100])
legend("RexCam", "Video-zilla", "RexCam", "Video-zilla")
save_plot_as_rexcam(f, "end-to-end-rexcam")