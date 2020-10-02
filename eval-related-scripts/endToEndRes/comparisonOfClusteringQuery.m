f = figure;
h = bar([88.7, 87.1; 91.1, 89.9; 91.5, 90.5; 92.1, 91.4]);
h(1).FaceColor = 'k';
h(2).FaceColor = 'w';
set(gca, 'XTickLabel', {'MobileNetV2', 'ResNet50', 'ResNet101', 'InceptionV3'});
legend("Video-zilla", "Spatial-correlation");
xlabel("Models");
ylabel("Classification accuracy (%)");
save_plot_as(f, "end-to-end-specialized-training");