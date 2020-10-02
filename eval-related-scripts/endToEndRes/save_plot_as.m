function save_plot_as(fig, filename, wide, dims)
%
%   format and save the fig 

%% Setup up variables
margin = 0.05;
position_for_export = [0   0   239   128];

%% Check args
error(nargchk(2,4,nargin));
if nargin == 2
    wide = 2;
    dims = [5.5 3]; % "ideal" aspect ratio for 2 graphs horizontally
elseif nargin == 3
    dims = [5.5 (3*wide/2)]; % scale "ideal" ratio to maintain vertical height
end
% Normalize to a document 6.5 inches wide (as it will be in the paper)
% .. This makes the font sizes and line widths used below accurate
dims = dims * (6.5 / wide / dims(1));
dims = dims - margin;

%% Save visual position
old_pos = get(fig, 'Position');

%% Set up plot size
set(fig, 'WindowStyle', 'Normal');
set(fig, 'Position', position_for_export);
set(fig, 'PaperUnits', 'inches');
set(fig, 'PaperSize', dims);
set(fig, 'PaperPositionMode', 'manual');
set(fig, 'PaperPosition', [margin/2, margin/2, dims - margin]);

%% Set up line
figure(fig);
lines = get(gca, 'Children');
for i = 1:length(lines)
    if strcmp(get(lines(i), 'Type'), 'line')
        if strcmp(get(lines(i), 'LineStyle'), 'none')
            set(lines(i), 'LineWidth', 0.5);
        else
            set(lines(i), 'LineWidth', 1.5);
        end
        set(lines(i), 'MarkerSize', 5);
    end
end

%% Set up fonts
ax = gca();
set(ax, 'FontName', 'Arial');
set(ax, 'FontSize', 8);
lab = get(ax, 'xlabel');
set(lab, 'FontName', 'Arial');
set(lab, 'FontSize', 8);
lab = get(ax, 'ylabel');
set(lab, 'FontName', 'Arial');
set(lab, 'FontSize', 8);

% leg = legend;
% set(leg, 'FontSize', 7);
% set(leg,'Location', 'Best');

%% Do the saving
%saveas(fig,sprintf('%s.fig', filename),'fig');
%saveas(fig,sprintf('%s.eps', filename),'psc2');
saveas(fig,sprintf('%s.pdf', filename),'pdf');

%% Restore visual position
set(fig, 'Position', old_pos);

end

